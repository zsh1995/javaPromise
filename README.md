# java实现Promise风格的异步回调
## 调用方法
### 基本用法
```
new Promise((resolv, reject)-> {
    asycMethodWithCallback(/*回调方法 */ (val)-> {
        resolv.accept(val);
    })
});
```
### 串联多个异步方法
```
new Promise<Integer>((resolv, reject)-> {
    asycMethodWithCallback1(/* 回调方法 */ (val)-> {
        resolv.accept(val); // assume val is 10
    })
}).then((preValue) -> {
    return new Promise<Integer>((resolv, reject)-> {
        asycMethodWithCallback2(/* 回调方法 */ (val)-> {
            resolv.accept(val + 1); // val is 10
        });
    })
}).then((preValue) -> {
      return new Promise<Integer>((resolv, reject)-> {
          asycMethodWithCallback3(/* 回调方法 */ (val)-> {
            resolv.accept(val); // val is 11
          })
       });
  })

```
## todo-list
- 异常处理
- 其它简化方法