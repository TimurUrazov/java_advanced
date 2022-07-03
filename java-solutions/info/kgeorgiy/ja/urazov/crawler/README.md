## Web Crawler

**Task**: 

1. Write a thread-safe WebCrawler class that will recursively crawl sites.
    1. The `WebCrawler` class must have the followong constructor:
    ```
    public WebCrawler constructor (Downloader downloader, int downloads, int extractors, int perHost);
    ```
    - `downloader` allows you to download pages and extract links from them;
    - `downloads` — maximum number of pages downloaded at the same time;
    - `extractors` — the maximum number of pages from which links are extracted at the same time;
    - `perHost` — the maximum number of pages loaded simultaneously from a single host. To determine the host, use the `getHost` method of the `URLUtils` class from the tests.
2. The WebCrawler class must implement the `Crawler` interface:
    ```
    public interface Crawler extends AutoCloseable interface {
    Result download(String url, int depth);

    void close();
    }
    ```
    
    - The `download` method should recursively crawl pages starting from the specified URL to the specified depth and return a list of downloaded pages and files. For example, if the depth is 1, then only the specified page should be loaded. If the depth is 2, then the specified page and the pages and files it links to, and so on. This method can be called in parallel in multiple threads.
    - Page loading and processing (link extraction) should be performed as much as possible in parallel, taking into account the restrictions on the number of pages loaded simultaneously (including from a single host) and the pages from which links are loaded.
    - For parallelization, it is allowed to create up to downloads + extractors auxiliary threads.
    - It is forbidden to `download` and/or extract links from the same page within the same crawl (download).
    - The close method must terminate all auxiliary threads. 
  3. To load pages, the `Downloader` passed by the first argument of the constructor must be used.
    ```
    public interface Downloader {
        public Document download(final String url) throws IOException;
    }
    ```                            

    - The `download` method loads the document at its [URL](http://tools.ietf.org/html/rfc3986). 
      The document allows you to get links on the loaded page:
      ```
      public interface Document {
        List<String> extractLinks() throws IOException;
      }
      ```

    - The links returned by the document are absolute and have an http or https schema. 
      
4. The main method must be implemented to run the crawl from the command line.
    - Command Line:
        ```
        WebCrawler url [depth [downloads [extractors [perHost]]]]
      ```
      
    - To load pages, you need to use the `CachingDownloader` implementation from the tests.
5. **Task versions**
    - restrictions on the number of simultaneous downloads from a single host (perHost >= downloads) must be taken into account. 
