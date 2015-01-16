var React        = require('react'),
    AppComponent = require('./react/app.jsx')
setTimeout(function(){
  React.render(<AppComponent msg1="Hello" msg2="This is client side react" />, document.getElementById("content"));
},1000);

