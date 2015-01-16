var React = require('react');
var AppComponent = React.createClass({
  render: function() {
    return (
      <div className="appComponent">
        <p>AppComponent</p>
        <p>{this.props.msg1}</p>
        <p>{this.props.msg2}</p>
      </div>
    );
  }
});
module.exports = AppComponent