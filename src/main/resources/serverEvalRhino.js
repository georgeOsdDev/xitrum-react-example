var dependencies = arguments[0],
    evalString   = arguments[1]
    ;

var global  = this;
var modules = JSON.parse(dependencies);
Object.keys(modules).forEach(function(k){
  global[k] = require(modules[k]);
});
print(eval(evalString));