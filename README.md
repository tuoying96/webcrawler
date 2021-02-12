# Project 1 webcrawler

[Project1: webcrawler](https://course.ccs.neu.edu/cs5700sp21/project2.html)
#### Team Members: Diwei Zhu, Ying Tuo

#### High-level Approach 
There are four steps to build this webcrawler:
1. Login *Fakebook* with the given userID and password;
2. Get the response from the *Fakebook*, and then parse and handle the [HTTP status codes](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes);
3. Crawl the pages within host *Fakebook* with **BFS**;
4. Close the socket and exit the program after we get five sercet flages or crawl every pages in *Fakebook*.
  
In details, we create a socket and using PrintWriter and BufferReader in Java to send POST and handel responses from the host.  
In `login()` method, we have the host `webcrawler-site.ccs.neu.edu` and the loginPath `http://" + [host] + "/accounts/login/?next=/fakebook/`. sessionid, then send a post request to server with username and password. If everthing is ok, we should get a response showing that login successful and a new cookie and sessionid will be created. 

#### Challenges
- First we assumed that every page will return a response with `cookie` and `sessionID`, while during the *login* page, it will only return the `cookie`, no `sessionID`. We solved this issue by examine the response and fingure out the format of response;
- The second thing is not a challenge but it is interesting. We can set the `User-Agent` request header for Web crawler to identify(or hide) itself to a web server in an HTTP request. In our program, we set `"User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10.16; rv:86.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36"`