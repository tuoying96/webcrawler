import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


/*
 * This class is a web crawler.
 * It provides only a main function to be called from command line with two parameters: username and password
 */
public class Crawler {
	// The information used to send request to the server
	private String username;
	private String password;
	private String host;
	private String loginPath;
	private String currentPath;

	// socket and iostream
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	// The information got from the server
	private String response;
	private String cookie;
	private String sessionID;
	private String cookieHeader;
	private boolean cookieUpdated;
	private String statusCode;

	// The container used to manage the paths
	private Deque<String> toVisit;
	private Set<String> visited;

	// The flags we want to find
	private Set<String> flags;

	// The regex patterns used to parse the response
	private static final String patternStrStatusCode = "HTTP/1.1 (\\p{Digit}+) .*?";
	private static final String patternStrCookieHeader = "Set-Cookie:\\s+([^\n]*)";
	private static final String patternStrCookie = "csrftoken\\s*=([^(\n;)]*)";
	private static final String patternStrSessionID = "sessionid\\s*=([^(\n;)]*)";
	private static final String patternStrFlag = "<h2 class=\'secret_flag\' style=\"color:red\">FLAG: (\\p{Alnum}{64})</h2>";
	private static final String patternStrUrl = "<a\\s+href=\"(/fakebook.*?)\"[^>]*>.*?</a>";
	private static final String patternStrLocation = "Location: ([^\n]+)";
	private static final String patternStrNewRef = "http://webcrawler-site.ccs.neu.edu([^(\\s\n;)]+)";

	/*
	 * Constructor
	 * @param username: a String as the username used to login
	 * @param passoword: a String as the password used to login
	 *
	 * Does: initialize all private attributes and connect to server.
	 */
	private Crawler(String username, String password) {
		this.username = username;
		this.password = password;

		this.host = "webcrawler-site.ccs.neu.edu";
		this.loginPath = "http://" + host + "/accounts/login/?next=/fakebook/";
		this.currentPath = this.loginPath;

		this.connectToServer();

		this.response = null;
		this.cookie = null;
		this.sessionID = null;
		this.cookieHeader = null;
		this.cookieUpdated = false;
		this.statusCode = null;

		this.toVisit = new LinkedList<> ();
		this.visited = new HashSet<> ();

		this.flags = new HashSet<> ();
	}

	/*
	 * connectToServer
	 *
	 * Does: try to connect to the server and set up iostream to the server. Quit on failture.
	 */
	private void connectToServer() {
		try {
			this.socket = new Socket(this.host, 80);
			this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			this.out = new PrintWriter(this.socket.getOutputStream());
		}
		catch (Exception e) {
			System.out.println("Failed connecting socket");
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * buildCookieHeader
	 *
	 * Does: build the cookie header for HTTP request if this.cookie or this.sessionID has been updated.
	 */
	public void buildCookieHeader() {
		// Do nothing if the cookie has not been updated
		if (!this.cookieUpdated) {
			return;
		}

		StringBuilder headerBuilder = new StringBuilder();
		if (this.cookie != null) {
			headerBuilder.append("Cookie: csrftoken=" + this.cookie);
			if (this.sessionID != null) {
				headerBuilder.append("; sessionid=" + this.sessionID);
			}
		}
		else if (this.sessionID != null) {
			headerBuilder.append("Cookie: sessionid=" + this.sessionID);
		}
		this.cookieHeader = headerBuilder.toString();
		this.cookieUpdated = false;
		return;
	}

	/*
	 * getResponse
	 *
	 * Does: from this.in, read out all information and build the response.
	 */
	public void getResponse() {
		StringBuilder responseBuilder = new StringBuilder();

		try {
			while (true) {
				responseBuilder.append(this.in.readLine());
				responseBuilder.append("\n");
				if (!this.in.ready()) {
					break;
				}
			}
		}
		catch (Exception e) {
			System.out.println("ERROR: GETRESPONSE: READLINE");
			e.printStackTrace();
			System.exit(1);
		}

		this.response = responseBuilder.toString();
	}

	/*
	 * sendGET
	 *
	 * Does: send a GET to the server, use the corresponding private attributes of this instance to build up the message
	 */
	public void sendGET() {
		this.buildCookieHeader();

		try {
			this.out.println("GET " + this.currentPath +  " HTTP/1.1");
			this.out.println("Host: " + this.host);
			this.out.println("Connection: keep-alive");
			this.out.println("User: zhu.diw@northeastern.edu & tuo.y@northeastern.edu");
			if (this.cookieHeader != null) {
				this.out.println(this.cookieHeader);
			}
			this.out.println("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/37.0.2062.120 Safari/537.36");
			this.out.println("");
			this.out.flush();
		}
		catch (Exception e) {
			System.out.println("ERROR: GET: OUT");
			e.printStackTrace();
			System.exit(1);
		}

		this.getResponse();

	}

	/*
	 * sendPOST
	 *
	 * Does: send a POST to the server, use the corresponding private attributes of this instance to build up the message
	 */
	public void sendPOST() {
		this.buildCookieHeader();
		String data = "username=" + this.username + "&password=" + this.password + "&csrfmiddlewaretoken=" + this.cookie + "&next=";

		try {
			this.out.println("POST " + this.currentPath +  " HTTP/1.1");
			this.out.println("Host: " + this.host);
			this.out.println("Connection: keep-alive");
			this.out.println("User: zhu.diw@northeastern.edu & tuo.y@northeastern.edu");
			this.out.println("Content-Type: application/x-www-form-urlencoded; charset=utf-8");
			this.out.println("Content-Length: " + data.length());
			if (this.cookieHeader.length() > 0) {
				this.out.println(this.cookieHeader);
			}
			this.out.println("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.16; rv:86.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
			this.out.println("");
			this.out.println(data);
			this.out.println("");
			this.out.flush();
		}
		catch (Exception e) {
			System.out.println("ERROR: POST: OUT");
			e.printStackTrace();
			System.exit(1);
		}

		this.getResponse();
	}

	public List<String> matchPattern(String target, String patternStr) {
		Pattern pattern = Pattern.compile(patternStr);
		Matcher matcher = pattern.matcher(target);

		List<String> matched = new ArrayList<> ();

		while (matcher.find()) {
			matched.add(matcher.group(1));
		}

		return matched;
	}
	/*
	 * getStatusCode
	 *
	 * Does: get the status code, which shoudl be after HTTP/1.1 in the HTTP response, and set this.statusCode correspondingly.
	 */
	public void getStatusCode() {
		List<String> matchedStatusCode = this.matchPattern(this.response, this.patternStrStatusCode);

		if (matchedStatusCode.size() == 0) {
			this.connectToServer();

			if (this.socket.isClosed()) {
				System.out.println("Failed reconnecting socket");
				System.exit(1);
			}

			this.statusCode = null;
		}
		else {
			this.statusCode = matchedStatusCode.get(0);
		}

		return;
	}

	/*
	 * processResponse
	 *
	 * Does: try to find any cookie and sessionid from the response
	 *       try to find any secret flag from the response
	 *       try to find any url starting with /fakebook from the response
	 */
	public void processResponse() {

		// this part attempts to find cookie and sessionid
		List<String> matchedCookieHeader = this.matchPattern(this.response, this.patternStrCookieHeader);

		for (int i = 0; i < matchedCookieHeader.size(); i++) {
			String cookieHeader = matchedCookieHeader.get(i);
			List<String> matchedCookie = this.matchPattern(cookieHeader, this.patternStrCookie);

			if (matchedCookie.size() > 0) {
				this.cookie = matchedCookie.get(0);
				this.cookieUpdated = true;
			}

			List<String> matchedSessionID = this.matchPattern(cookieHeader, this.patternStrSessionID);

			if (matchedSessionID.size() > 0) {
				this.sessionID = matchedSessionID.get(0);
				this.cookieUpdated = true;
			}
		}

		// this part attempts to find flags
		List<String> matchedFlag = this.matchPattern(this.response, this.patternStrFlag);
		for (String flag : matchedFlag) {
			flags.add(flag);
		}

		// this part get urls from the response
		List<String> matchedUrl = this.matchPattern(this.response, this.patternStrUrl);

		for (int i = 0; i < matchedUrl.size(); i++) {
			if (this.visited.contains(matchedUrl.get(i))) {
				continue;
			}

			this.toVisit.addLast(matchedUrl.get(i));
		}
	}

	/*
	 * login
	 *
	 * Does: send a GET to the login page to get a cookie, then send a POST to the login page the get a new cookie and a session id.
	 */
	public void login() {
		this.sendGET();
		this.getStatusCode();

		if (!this.statusCode.equals("200")) {
			this.sendGET();
			this.getStatusCode();
			if (!this.statusCode.equals("200")) {
				System.out.println("ERROR: LOGIN GET");
				System.exit(1);
			}
		}

		this.processResponse();
		this.sendPOST();
		this.getStatusCode();

		if (!this.statusCode.equals("302")) {
			System.out.println("ERROR: LOGIN POST");
			System.out.println("Status code should be 302 but is: " + this.statusCode);
			System.exit(1);
		}

		this.processResponse();
	}

	/*
	 * findNewRef
	 *
	 * Does: find new references in the Location field when encountering 301.
	 */
	private void findNewRef() {
		List<String> locationList = this.matchPattern(this.response, this.patternStrLocation);

		for (int i = 0; i < locationList.size(); i++) {
			List<String> newRefList = this.matchPattern(locationList.get(i), this.patternStrNewRef);
			for (int j = 0; j < newRefList.size(); j++) {
				if (!this.visited.contains(newRefList.get(j))) {
					this.toVisit.addLast(newRefList.get(j));
				}
			}
		}
	}

	/*
	 * go
	 * Used BFS Algorithm to crawl the website
	 * Does: start crawling from the root path "/"
	 */
	public void go() {
		String rootPath = "/";

		this.toVisit.addLast(rootPath);

		while (this.toVisit.size() > 0 && this.flags.size() < 5) {
			this.currentPath = this.toVisit.poll();
			if (this.visited.contains(this.currentPath)) {
				continue;
			}
			this.sendGET();
			this.getStatusCode();

			// If server throws a 500 - Internal Server Error, re-try the request for the URL until the request is successful
			if (this.statusCode == null || this.statusCode.equals("500")) {
				this.toVisit.addFirst(currentPath);
				continue;
			}

			// Have visited the page, add to visitedPages.
			this.visited.add(this.currentPath);

			// Is server returns 301 - Moved Permanently, our crawler should try the request again using the new URL given by the server.
			if (this.statusCode.equals("301")) {
				this.findNewRef();
				continue;
			}

			// If server returns reponse with errors 403 Forbidden and 404 - Not Found, just abandon the URL.
			if (this.statusCode.equals("403") || this.statusCode.equals("404")) {
				continue;
			}
			this.processResponse();
		}
	}

	/*
	 * showFlags
	 *
	 * Does: show the content in this.flags, which should be 5 64bit strings on success.
	 */
	public void showFlags() {
		for (String flag : flags) {
			System.out.println(flag);
		}
	}

	/*
	 * shutDown
	 *
	 * Does: close the socket and the iostreams
	 */
	public void shutDown() {
		try {
			this.out.close();
			this.in.close();
			this.socket.close();
		}
		catch (Exception e) {
			System.out.println("ERROR: SHUTDOWN");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		String username = args[0];
		String password = args[1];

		Crawler crawler = new Crawler(username, password);

		crawler.login();

		crawler.go();

		crawler.shutDown();

		crawler.showFlags();

		return;
	}
}
