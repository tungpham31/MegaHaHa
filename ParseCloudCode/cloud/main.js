
// Use Parse.Cloud.define to define as many cloud functions as you want.
// For example:
Parse.Cloud.define("hello", function(request, response) {
  response.success("Hello world!");
});

Parse.Cloud.define("updateScore", function(request, response) {
	var video = undefined;
	var query = new Parse.Query("Video");
  	query.equalTo("videoId", request.params.videoId);
  	query.find({
    	success: function(results) {
      		video = results[0];
      	},
    	error: function() {
      		response.error("movie lookup failed");
   		 }
 	});

  	if (video !== undefined){
		var score = video.get("score");
		var like = video.get("like");
		var view = video.get('view');
		var score2 = 0;

		if (score > 0){
			score2 = score * (like + 1) / (view + 1);
		}
		else score2 = score * (view + 1) / (like + 1);

		video.set("score2", score2);
		video.save();
	}
});
