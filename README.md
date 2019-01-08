# java实现Promise风格的异步回调
## 调用方法
### 基本用法
``` java
new Promise((resolv, reject)-> {
    asycMethodWithCallback(/*回调方法 */ (val)-> {
        resolv.accept(val);
    })
});
```
### 串联多个异步方法
``` java
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
## 定时任务
- `long setTimeout(Consumer, value, timeDelay)`<br>
在 `timeDelay`毫秒后，将值 `value` 传入 `consumer` 消费。该方法返回一个 `long` 类型的 `id` ，可根据这个 `id` 取消定时任务。<br>
- `clearTimeout(id)`<br>
根据任务id，取消定时任务。（采用懒删除机制，只是使任务定时到后不执行）
- `long setInterval(Consumer, value, interval)`<br>
任务将以 `interval` 毫秒为间隔进行调用。返回任务id值。
- `clearInterval(id)`<br>
取消任务。

## todo-list
- 其它简化方法