var React = require('react'),
    App   = React.createFactory(require('./react/app.jsx'))

var appProps = JSON.parse(document.getElementById('App-props').getAttribute('data-json'));
setTimeout(function(){
  React.render(App(appProps), document.getElementById("content"));
},3000);
