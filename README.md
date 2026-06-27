# Tank Battle - 坦克大战

一个用 Java Swing 编写的经典坦克大战游戏。保护你的基地，消灭所有敌方坦克！

## 截图

游戏界面包含 15×15 的地图，玩家坦克（绿色）从底部出发，敌方坦克（红色）从顶部三个出生点刷新。

## 玩法

- **目标**：消灭全部 12 辆敌方坦克，同时保护基地（E）不被摧毁
- **生命**：玩家有 3 条命，被击中后从底部中央重生
- **失败条件**：生命耗尽或基地被摧毁
- **胜利条件**：消灭所有敌方坦克

## 操作

| 按键 | 功能 |
|------|------|
| W A S D / 方向键 | 移动 |
| 空格 | 开火 |
| P | 暂停 / 继续 |
| R | 重新开始 |
| ESC | 退出 |

## 运行

### 方式一：直接编译运行

```bash
javac -encoding UTF-8 -d out src/*.java
java -cp out TankBattle
```

### 方式二：打包为 JAR 后运行

Windows CMD:
```bash
build.bat
run.bat
```

Windows PowerShell:
```powershell
.\build.ps1
java -jar TankBattle.jar
```

### 方式三：IDE 中运行

在 IntelliJ IDEA 等 IDE 中直接运行 `src/Main.java`。

## 项目结构

```
├── src/
│   ├── Main.java          # 调试入口
│   └── TankBattle.java    # 游戏主类（含全部逻辑与渲染）
├── Manifest.txt            # JAR 清单文件
├── build.bat               # Windows 打包脚本 (CMD)
├── build.ps1               # Windows 打包脚本 (PowerShell)
├── run.bat                 # 运行 JAR 脚本
└── .gitignore
```

## 技术栈

- Java (Swing / AWT)
- 无第三方依赖，纯标准库实现
