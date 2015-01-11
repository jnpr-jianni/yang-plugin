# yang-plugin
=========================

#### Import yang plugin

In plugins.sbt, add:

```
resolvers += Resolver.url(
  "jspace-private-plugins",
  url("http://10.155.87.253:8080/mavenrepo/sbt"))(Resolver.ivyStylePatterns)

addSbtPlugin("net.juniper" % "yang-plugin" % "0.1.1")
```

### Configure yang plugin

```
import net.juniper.yang.YangPlugin

YangPlugin.yangPackageName := Option("net.juniper.yang")

settings ++ YangPlugin.yangSettings
```

### Publish yang plugin

```
mount //10.155.87.253:8080/mavenrepo/sbt ~/mavenrepo
sbt publish
```
Please be careful since the mounted repo is for development and build reference. It is recommended to mount the samba directory to another location and then copy the published files to it after it is verified.