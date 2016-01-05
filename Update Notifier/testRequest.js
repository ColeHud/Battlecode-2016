var request = require('request');
var currentVersion = "0.0.1";

var address = "http://www.battlecode.org/contestants/releases/";
request(address, function (error, response, body) {
		if (!error && response.statusCode == 200) 
		{
			//get the website version
			var beginning = "<strong>";
			var startIndex = body.indexOf(beginning) + beginning.length;
			var endIndex = body.indexOf("</strong>");

			var websiteVersion = body.slice(startIndex, endIndex);

			//if it doesn't equal the currentVersion variable, send a text to notify
			if(websiteVersion != currentVersion)
			{
				currentVersion = websiteVersion;
				console.log(currentVersion);
			}
  		}
	})