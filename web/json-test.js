// var host = "172.30.11.167";
Context.USERNAME = 'user###';
Context.PASSWORD = 'pass~~~';
var host = "127.0.0.1";
var opt1 = {
    url: 'http://' + host + ':19080/bus'
};

opt1.tx = 'API-PERSON_001';
opt1.arguments = ['310226198101212314'];
opt1.callback = function (person) {
    console.info('Service echo: ' + person.name + '{'
    + person.identificationNumber + '}');
};
ALBus.invoke(opt1);

var opt2 = {
    url: 'http://' + host + ':19080/bus'
};
opt2.tx = 'API-PERSON_003';
opt2.arguments = ['*:*', 10, 1];
opt2.callback = function (persons) {
    console.info('Service echo: ' + persons.length + ' persons.');
    for (var i = 0; i < persons.length; i++)
        console.info('==person [' + i + ']: ' + persons[i].name + "{"
        + persons[i].identificationNumber + "}");
};
ALBus.invoke(opt2);
