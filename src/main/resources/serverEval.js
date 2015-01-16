var args         = process.argv,
    engine       = args[0],
    file         = args[1],
    dependencies = args[2],
    evalString   = args[3]
    ;

var global = this;
var modules = JSON.parse(dependencies);
Object.keys(modules).forEach(function(k){
  global[k] = require(modules[k]);
});

console.log(eval(evalString));