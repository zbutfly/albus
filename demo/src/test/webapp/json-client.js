//debugger;
var arguments = {
	value : 'hello',
	values : [ 5, 4, 3, 2, 1 ]
};
var option = {
	type : 'post',
	data : JSON.stringify(arguments),
	success : function(data) {
		console.info('Service echo: ' + data.result.title);
	},
	// constants
	contentType : 'application/json',
	accepts : {
		json : 'application/json'
	},
	processData : false,
	dataType : 'json',
	headers : {
		'X-BUS-TX' : 'TST_CMT-002',
		'X-BUS-Context-SourceAppID' : 'JSON-DEMO',
		'X-BUS-Context-USERNAME' : 'user',
		'X-BUS-Context-PASSWORD' : 'pass'
	}
};
$.ajax('http://127.0.0.1:19080/bus', option);