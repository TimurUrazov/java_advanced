package info.kgeorgiy.ja.urazov.crawler;

/**
 * Exception occurred while crawling web-site by {@link WebCrawler}.
 */
public class CrawlException extends RuntimeException {
    CrawlException(final String message) {
        super(message);
    }
}
