var args         = process.argv,
    engine       = args[0],
    file         = args[1],
    dependencies = args[2],
    evalString   = args[3]
    ;

var React = require('react');
require('node-jsx').install({harmony: true, extension: '.jsx'})
var global = this;
var modules = JSON.parse(dependencies);
Object.keys(modules).forEach(function(k){
  global[k] = React.createFactory(require(modules[k]));
});
var ret = eval("(function(){ return "+evalString+"})();");
console.log(ret);