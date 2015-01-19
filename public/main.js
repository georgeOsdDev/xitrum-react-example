var App = React.createClass({
  getInitialState: function() {
    return {
      cnt: 0
    };
  },

  toggleClicked: function() {
    this.setState({
      cnt: this.state.cnt +1
    });
  },

  componentWillMount:function() {
  },

  componentDidMount:function() {
    console.log("didMount");
  },

  render: function() {
    return (
      <div className="appComponent" >
      <p>App</p>
      <p>{this.props.msg1} {this.props.msg2}</p>
      <p>
      <button onClick={this.toggleClicked}></button>
      <span>{this.state.cnt}</span>
      </p>
      </div>
    );
  }
});
var appProps = JSON.parse(document.getElementById('App-props').getAttribute('data-json'));
React.render(App(appProps), document.getElementById("content"));
