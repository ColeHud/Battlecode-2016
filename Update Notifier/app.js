var request = require('request');
var address = "http://www.battlecode.org/contestants/releases/";
var currentVersion = "0.0.1";

// Twilio Credentials 
var accountSid = 'AC875dd9af14da990ead957aeb8f673598'; 
var authToken = '289c8aa340cb8c86e48b219afa426e02'; 

//require the Twilio module and create a REST client 
var client = require('twilio')(accountSid, authToken); 

var notify = function()
{
	console.log("New Battlecode version!!! " + currentVersion );

	client.messages.create({ 
		to: "2314140348", 
		from: "+12316741066", 
		body: "The Battlecode version has been updated to " + currentVersion
	}, function(err, message) { 
		console.log("Message sent");
	});
}

//check battlecode website every 30 seconds
setInterval(function(){
	//request html body
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
				notify();
			}
  		}
	})

}, 30000);