# React native plugin for Read SMS
	This plugin is used to read any new upcoming SMS and read previous sms. 

### Mostly automatic installation

To enable `Read SMS` feature you have to add the following code to the `AndroidManifest.xml`:

```java
  <uses-permission android:name="android.permission.RECEIVE_SMS" />
  <uses-permission android:name="android.permission.READ_SMS" />
```