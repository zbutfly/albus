//debugger;
var arguments = ["hello",[5,4,3,2,1]];
var option = {
	// from parameter
	url:"http://127.0.0.1:9876/bus/TST_CMT-002", 
	data:JSON.stringify(arguments),
	success: function(data) {
		console.log("Service echo: " + data.result.title);
	},
	// constants
	type:"POST",
	contentType: 'application/json',
	accepts:{
		json: 'application/json'
	}, 
	processData: false,
	dataType:"json",
	beforeSend: function(xhr) {
		xhr.overrideMimeType( "application/json; charset=utf-8" );
		// from parameter
		xhr.setRequestHeader("X-BUS-Context", "{SourceAppID: 'JSON-DEMO'}");
		//xhr.setRequestHeader("X-BUS-TX", "TST_CMT-002");
		//xhr.setRequestHeader("X-BUS-TX-Version", "*");
	}
};
$.ajax(option);