# getTamperSwitch
测试背光，IR 测试

### 导入项目后需要修改HR40包的位置，在`build.gradle`中进行指定
```shell
 gradle.projectsEvaluated {
        tasks.withType(JavaCompile) {
           //options.compilerArgs.add('-Xbootclasspath/p:D:\\Andy\\elan_master\\nsc_elan\\GVIEWER_ANDROID\\GVIEWER_HR40\\app\\libs\\HR40.jar')
            options.compilerArgs.add('-Xbootclasspath/p:D:/Andy/android_project/getTamperSwitch/app/libs/HR40.jar')
           //options.compilerArgs.add('-Xbootclasspath/p:app\\libs\\HR40_KeyEvent.jar')  // 相对路径不成功. 最新版本 Android Studio 成功.
        }
    }
```

最好是些绝对路径，测试过相对路径有时候会访问不到
