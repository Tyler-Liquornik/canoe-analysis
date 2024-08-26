### Setting up Tracing with AspectJ

AspectJ is a special extension to java which implements the AOP (Aspect Oriented Programming) paradigm. <br/>
AspectJ requires its own compiler, ajc to be used in tandem with the standard java compiler, javac <br/>
With AspectJ, a powerful, customizable tracing featured built into PADDL can be enabled for an enhanced debugging developer experience <br/>
The plugin for AspectJ requires IntelliJ Ultimate. It is complete opt-in and PADDL should build, compile, deploy, and run fine without it. <br/>

<div style="display: flex; flex-direction: column; width: 80%; align-items: center; justify-content: center; padding: 10px 0 10px 0">
      <img src="../../../../../../../images/tracing.png" alt="tracing" /> <br/>
</div>
<br/>
<b>a) Install the AspectJ plugin </b>
<div style="display: flex; flex-direction: column; width: 80%; align-items: center; justify-content: center; padding: 10px 0 10px 0">
      <img src="../../../../../../../images/aj-plugin.png" alt="aj-plugin" /> <br/>
</div>
<br/>
<b>b) Select the ajc compiler</b>
<div style="display: flex; flex-direction: column; width: 80%; align-items: center; justify-content: center; padding: 10px 0 10px 0">
      <img src="../../../../../../../images/ajc.png" alt="ajc" /> <br/>
</div>
<br/>
<b>c) Enable post-compile weave mode</b>
<div style="display: flex; flex-direction: column; width: 80%; align-items: center; justify-content: center; padding: 10px 0 10px 0">
      <img src="../../../../../../../images/weaving.png" alt="weaving" /> <br/>
</div>
<br/>
<b>d) Customize tracing behaviour</b> <br/>
run <code>maven clean install</code> to build once, then change the <code>tracing</code> property in <code>dev-config.yaml</code> to enable or disable tracing. <br/>
Use <code>@Traceable</code> and <code>@TraceIgnore</code> for fine-grained customization on which methods are traced.
<div style="display: flex; flex-direction: column; width: 80%; align-items: center; justify-content: center; padding: 10px 0 10px 0">
      <img src="../../../../../../../images/tracing-config.png" alt="tracing-config" /> <br/>
</div>