package info.kgeorgiy.ja.urazov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class WebCrawler implements AdvancedCrawler {
    private static final int DEFAULT_TIMEOUT_MILLIS = 500;
    private static final int[] DEFAULT_PARAMS = new int[]{1, 2, 2, 2};

    // :NOTE: явный HashMap
    private final Downloader downloader;
    private final int perHost;
    private final Map<String, Semaphore> hosts;
    private final Set<Thread> threads;
    private final Map<String, BlockingQueue<String>> waitingTasks;

    private final ExecutorService downloadersExecutor;
    private final ExecutorService extractorsExecutor;

    private boolean isClosed;

    private static ExecutorService createExecutor(final int threadNum) {
        // :NOTE: Executors.newFixedThreadPool()
        return Executors.newFixedThreadPool(threadNum);
    }

    /**
     * Creates an instance of {@code WebCrawler}
     *
     * @param downloader instance that allow to download pages by URL
     * @param downloaders upper bound on number of pages being downloaded simultaneously
     * @param extractors upper bound on number of pages being extracted simultaneously
     * @param perHost upper bound on number of pages being downloaded or extracted from one host
     *
     * @see Downloader
     */
    public WebCrawler(final Downloader downloader,
                      final int downloaders,
                      final int extractors,
                      final int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        hosts = new ConcurrentHashMap<>();
        threads = ConcurrentHashMap.newKeySet();
        waitingTasks = new HashMap<>();
        downloadersExecutor = createExecutor(downloaders);
        extractorsExecutor = createExecutor(extractors);
    }

    /**
     * Downloads web site up to specified depth.
     * Throws {@link CrawlException} if Runtime exception occurred or crawler was shutdown.
     *
     * @param url start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @return download result.
     */
    @Override
    public Result download(String url, int depth) {
        return download(url, depth, (Set<String>) null);
    }

    private Result download(final String url,
                            final int depth,
                            final Set<String> hostRestriction) {
        if (downloadersExecutor.isShutdown()) {
            throw new CrawlException("Crawler has been shut down, can not download");
        }

        final Set<String> visited = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> failed = new ConcurrentHashMap<>();

        final Phaser phaser = new Phaser(1) {
            @Override
            protected boolean onAdvance(int phase, int registeredParties) {
                return phase == depth - 1;
            }
        };

        threads.add(Thread.currentThread());

        final Set<String> currentLinks = ConcurrentHashMap.newKeySet();
        currentLinks.add(url);

        final ExceptionHandler crawlException = new ExceptionHandler();

        while (!phaser.isTerminated()) {
            final List<List<String>> urls = currentLinks
                    .stream()
                    .filter(u -> !visited.contains(u) && !failed.containsKey(u))
                    .flatMap(u -> {
                        try {
                            final String host = URLUtils.getHost(u);
                            if (hostRestriction == null || hostRestriction.contains(host)) {
                                visited.add(u);
                                return Stream.of(List.of(u, host));
                            }
                        } catch (MalformedURLException e) {
                            failed.put(u, e);
                        }
                        return Stream.of();
                    })
                    .toList();

            phaser.bulkRegister(urls.size());

            urls.forEach(urlAndHost -> execute(
                    failed, phaser, currentLinks, crawlException,
                    urlAndHost.get(0), urlAndHost.get(1)
            ));

            try {
                phaser.awaitAdvanceInterruptibly(phaser.arrive());
            } catch (InterruptedException e) {
                if (!isClosed) {
                    close();
                    Thread.currentThread().interrupt();
                }
                throw new CrawlException("Crawler has been shut down, can not download");
            }
        }

        crawlException.drop();
        visited.removeAll(failed.keySet());
        threads.remove(Thread.currentThread());

        return new Result(new ArrayList<>(visited), failed);
    }

    private void execute(final Map<String, IOException> failed,
                         final Phaser phaser,
                         final Set<String> currentLinks,
                         final ExceptionHandler crawlException,
                         final String url,
                         final String host) {
        final Semaphore loaded = hosts.computeIfAbsent(host, k -> new Semaphore(perHost));
        if (loaded.tryAcquire()) {
            downloadersExecutor.execute(() -> {
                downloaded(phaser, url, failed, currentLinks, crawlException);
                if (waitingTasks.get(host) != null) {
                    while (!waitingTasks.get(host).isEmpty()) {
                        downloaded(
                                phaser, waitingTasks.get(host).poll(),
                                failed, currentLinks, crawlException
                        );
                    }
                }
                loaded.release();
            });
        } else {
            waitingTasks.computeIfAbsent(host, k -> new LinkedBlockingQueue<>()).add(url);
        }
    }

    private void downloaded(final Phaser phaser,
                            final String url,
                            final Map<String, IOException> failed,
                            final Set<String> links,
                            final ExceptionHandler crawlException) {
        try {
            final Document document = downloader.download(url);
            extractorsExecutor.execute(() -> {
                try {
                    links.addAll(document.extractLinks());
                } catch (IOException e) {
                    failed.put(url, e);
                }
                phaser.arriveAndDeregister();
            });
        } catch (IOException e) {
            phaser.arriveAndDeregister();
            failed.put(url, e);
        } catch (RejectedExecutionException ignored) {
            phaser.arriveAndDeregister();
        } catch (RuntimeException e) {
            phaser.arriveAndDeregister();
            crawlException.processException(
                    new CrawlException("Error parsing " + url + ": " + e.getMessage())
            );
        }
    }

    /**
     * Closes this web-crawler, relinquishing any allocated resources.
     */
    @Override
    public void close() {
        terminate(extractorsExecutor);
        terminate(downloadersExecutor);
        isClosed = true;
        threads.forEach(Thread::interrupt);
    }

    private void terminate(final ExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(DEFAULT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static int[] getParams(final String[] args) {
        final int[] params = new int[4];
        IntStream.range(0, DEFAULT_PARAMS.length).forEach(i -> {
            try {
                params[i] = Integer.parseInt(args[i + 1]);
            } catch (final NumberFormatException e) {
                params[i] = DEFAULT_PARAMS[i];
            }
        });
        return params;
    }

    /**
     * The starting point of execution of {@link WebCrawler}, which runs
     * {@link #download(String, int)} and returns list of downloaded pages or
     * errors if they occurred
     *
     * @param args url [depth [downloads [extractors [perHost]]]], where
     *             url is start <a href="http://tools.ietf.org/html/rfc3986">URL</a>,
     *             depth is download depth,
     *             downloads is upper bound on number of pages being downloaded simultaneously,
     *             extractors is upper bound on number of pages being extracted simultaneously,
     *             perHost is upper bound on number of pages being downloaded or extracted from
     *             one host
     *
     */
    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 5) {
            System.err.println("Usage:\t WebCrawler url [depth [downloads [extractors [perHost]]]]");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Non-null arguments are required");
            return;
        }

        final int[] params = getParams(args);

        try (final WebCrawler webCrawler = new WebCrawler(
                new CachingDownloader(), params[1], params[2], params[3])) {
            Result result = webCrawler.download(args[0], params[0]);
            result.getDownloaded().forEach(p -> System.out.println("Downloaded: " + p));
            result.getErrors().forEach((p, e) ->
                    System.out.println("Downloaded: " + p + ", error: " + e.getMessage()));
        } catch (IOException e) {
            System.err.println("Can't create downloader");
        } catch (CrawlException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Downloads web site up to specified depth.
     * Throws {@link CrawlException} if Runtime exception occurred or crawler was shutdown
     *
     * @param url start <a href="http://tools.ietf.org/html/rfc3986">URL</a>.
     * @param depth download depth.
     * @param hosts domains to follow, pages on another domains should be ignored.
     * @return download result.
     */
    @Override
    public Result download(String url, int depth, List<String> hosts) {
        // :NOTE: Java heap space
        return download(url, depth, hosts.stream().collect(Collectors.toUnmodifiableSet()));
    }

    public final static class ExceptionHandler {
        private CrawlException exceptions;
        
        public ExceptionHandler() {
            exceptions = null;
        }

        public void processException(final CrawlException exception) {
            if (this.exceptions == null) {
                this.exceptions = exception;
            } else {
                this.exceptions.addSuppressed(exception);
            }
        }

        public boolean empty() {
            return exceptions == null;
        }
        
        public void drop() throws CrawlException {
            if (!empty()) {
                throw exceptions;
            }
        }
    }
}
