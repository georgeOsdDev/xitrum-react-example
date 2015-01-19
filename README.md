# Xitrum-React-Example

This is a React server side rendering example with Xitrum(Scala).
React is executed with [typesafehub/js-engine](https://github.com/typesafehub/js-engine) on Xitrum.
Javascript code(Saved in `src/resources/react/`) is isomorphic amd work both on client and server.


### How to run:

```sh
cd src/resources
npm install
browserify -t reactify main.js > ../../../public/bundle.js
cd ../../
sbt/sbt run
```


#Server-Side

### Action

ReactIndex.scala

```scala
@GET("react")
class ReactIndex extends Action with ReactRenderer {
  override def layout = renderViewNoLayout()
  def execute() {

    // Properties for React component
    val props = Map("msg1" -> "Hello", "msg2" -> "World")

    // Create react component
    // result html will be saved as reactForView(See also template jade file)
    // property will saved as propsForView(See also template jade file)
    renderReactView("App", props)

    // Respond as xitrum way
    respondView()
  }
}
```

### Template

ReactIndex.jade

```jade
!!! 5
html
  head
    meta(content="text/html; charset=utf-8" http-equiv="content-type")
    title Xitrum-React
    != currentAction.asInstanceOf[ReactIndex].propsForView
  body
    ! <div id="content">#{currentAction.asInstanceOf[ReactIndex].reactForView}</div>
    script(src={publicUrl("bundle.js")})
```


## Client-Side

main.js(will be browserify and reactify as bundle.js)

```javascript
var React = require('react'),
    App   = React.createFactory(require('./react/app.jsx'))

// Use exactly same property as server-side did.
// Client side javascript can took over pre-rendered HTML with React Component.
// So React will not re-render DOM, just add event listeners.
var appProps = JSON.parse(document.getElementById('App-props').getAttribute('data-json'));
React.render(App(appProps), document.getElementById("content"));
```


## Rendered HTML
```html
<!DOCTYPE html>
<html>
<head>
<meta content="text/html; charset=utf-8" http-equiv="content-type">
<title>Xitrum-React</title>
<script id="App-props" type="text/plain" data-react-class="App" data-json="{&quot;msg1&quot;:&quot;Hello&quot;,&quot;msg2&quot;:&quot;World&quot;}"></script>

</head>
<body>
<div id="content">
  <div class="appComponent" data-reactid=".2bhn40i2cxs" data-react-checksum="67606183">
    <div data-reactid=".2bhn40i2cxs.0">
      <p data-reactid=".2bhn40i2cxs.0.0">App</p>
      <p data-reactid=".2bhn40i2cxs.0.1">
        <span data-reactid=".2bhn40i2cxs.0.1.0">Hello</span>
        <span data-reactid=".2bhn40i2cxs.0.1.1"> </span>
        <span data-reactid=".2bhn40i2cxs.0.1.2">World</span>
      </p>
      <p data-reactid=".2bhn40i2cxs.0.2">
        <button data-reactid=".2bhn40i2cxs.0.2.0"></button>
        <span data-reactid=".2bhn40i2cxs.0.2.1">0</span>
      </p>
    </div>
  </div>
</div>
<script src="/bundle.js?1421658526000"></script>
</body>
</html>
```
