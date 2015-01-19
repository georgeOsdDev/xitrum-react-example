var React = require('react'),
    App   = React.createFactory(require('./react/app.jsx'))

var appProps = JSON.parse(document.getElementById('App-props').getAttribute('data-json'));
React.render(App(appProps), document.getElementById("content"));