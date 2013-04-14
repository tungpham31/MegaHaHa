
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.define("crawlVideos", function(request, response) {
	var url = "https://gdata.youtube.com/feeds/api/users/NqDlI2N-jKSr3yiipGVF_w/subscriptions?v=2";
	var www : WWW = new WWW (url);
 
	// wait for request to complete
	yield www;
 
	// and check for errors
	if (www.error == null)
	{
    	// request completed!
	} else {
   		// something wrong!
    	Debug.Log("WWW Error: "+ www.error);
	}
});
