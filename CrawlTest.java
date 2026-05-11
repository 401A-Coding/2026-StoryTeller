import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 爬虫测试 - 测试番茄小说爬取
 */
public class CrawlTest {
    public static void main(String[] args) {
        try {
            // 先试试番茄小说首页，看看能不能访问
            System.out.println("=== 测试1: 访问番茄小说首页 ===\n");
            Document homeDoc = Jsoup.connect("https://fanqienovel.com/")
                    .timeout(15000)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .get();

            System.out.println("首页标题: " + homeDoc.title());
            System.out.println("状态: 访问成功!\n");

            // 找找首页上的小说链接
            System.out.println("--- 首页上的小说链接 ---");
            Elements novelLinks = homeDoc.select("a[href*=/page/]");
            if (novelLinks.isEmpty()) {
                novelLinks = homeDoc.select("a[href*=/novel/]");
            }
            if (novelLinks.isEmpty()) {
                novelLinks = homeDoc.select("a[href]");
            }

            int count = 0;
            for (Element link : novelLinks) {
                String href = link.attr("href");
                String text = link.text().trim();
                if (text.length() > 0 && text.length() < 100
                    && (href.contains("/page/") || href.contains("/novel/") || href.contains("/book/"))) {
                    String fullUrl = href.startsWith("http") ? href : "https://fanqienovel.com" + href;
                    System.out.println("  [" + text + "]");
                    System.out.println("  URL: " + fullUrl);
                    System.out.println();
                    count++;
                    if (count >= 5) break;
                }
            }

            if (count == 0) {
                System.out.println("未找到小说链接，打印所有链接:");
                for (Element link : novelLinks) {
                    String href = link.attr("href");
                    String text = link.text().trim();
                    if (text.length() > 0 && text.length() < 50) {
                        System.out.println("  " + text + " -> " + href);
                    }
                }
            }

            // 如果找到了小说链接，尝试爬取第一个
            if (count > 0) {
                Element firstLink = novelLinks.get(0);
                String firstUrl = firstLink.attr("href");
                firstUrl = firstUrl.startsWith("http") ? firstUrl : "https://fanqienovel.com" + firstUrl;

                System.out.println("\n=== 测试2: 爬取小说详情页: " + firstUrl + " ===\n");
                Document detailDoc = Jsoup.connect(firstUrl)
                        .timeout(15000)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .get();

                System.out.println("详情页标题: " + detailDoc.title());

                // 打印所有文本内容的前3000字
                String bodyText = detailDoc.body().text();
                System.out.println("\n--- 页面内容预览 ---");
                System.out.println(bodyText.length() > 3000 ? bodyText.substring(0, 3000) : bodyText);
            }

        } catch (Exception e) {
            System.err.println("爬取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
