<p align="center">
  <img src="src/main/resources/gocstrike-logo.png" width="280" alt="GOC Strike Logo" />
</p>

# 奇术打击

客户端和服务器均需安装与 Minecraft 版本对应的 GOC Strike JAR。

[实现说明（NeoForge 1.21.1）](docs/implementation-1.21.1.md)

| Minecraft | 加载器构建 | Java | Gradle 任务 |
| --- | --- | --- | --- |
| 1.7.10 | Forge 10.13.4.1614 | 8 | `-p ports/forge-1.7.10 build` |
| 1.12.2 | Forge 14.23.5.2847（构建） | 8 | `-p ports/forge-1.12.2 build` |
| 1.19.3 | Forge 44.1.23 | 17 | `:forge-1.19.3:build` |
| 1.19.4 | Forge 45.4.5 | 17 | `:forge-1.19.4:build` |
| 1.20 | Forge 46.0.14 | 17 | `:forge-1.20:build` |
| 1.20.1 | Forge 47.4.28 | 17 | `:forge-1.20.1:build` |
| 1.21.1 | NeoForge 21.1.265 | 21 | `:build` |
| 1.21.2 | NeoForge 21.2.1-beta | 21 | `:neoforge-1.21.2:build` |
| 1.21.3 | NeoForge 21.3.100 | 21 | `:neoforge-1.21.3:build` |
| 1.21.4 | NeoForge 21.4.161 | 21 | `:neoforge-1.21.4:build` |
| 26.1 | NeoForge 26.1.0.19-beta | 25 | `:neoforge-26.1:build` |
| 26.1.1 | NeoForge 26.1.1.15-beta | 25 | `:neoforge-26.1.1:build` |
| 26.1.2 | NeoForge 26.1.2.129 | 25 | `:neoforge-26.1.2:build` |
| 26.2 | NeoForge 26.2.0.95 | 25 | `:neoforge-26.2:build` |
| 26.3 | NeoForge 26.3.0.115 | 25 | `:neoforge-26.3:build` |

## 使用

在创造模式的「工具与实用物品」栏获取「奇术打击」，或执行：

```mcfunction
/give @s gocstrike:thaumaturgic_strike
```

- 右键：以使用位置为固定中心，在头顶展开约 118.7 秒的完整法阵动画。
- 潜行右键：立即收起自己召唤的法阵。
- 每个维度同时播放一场；播放期间再次使用会显示提示。
- 物品可重复使用，不消耗。结束或卸载区块后清理法阵；退出世界不会保存未完成的法阵。

建议在开阔地观看，仰视中央法阵，或退开约 40–70 格观看全景。法阵沿用原效果的 2.4 倍世界比例，主要结构位于使用点上方数十格。

动画保留外围符印、悬浮晶体、逐段反射、分层旋转法阵、汇聚光束、冲击环和末尾白光。客户端在两帧姿态间插值，仅同步一个效果实体。实体笔画使用保留色相的无光照渲染，再由独立发光层经过两级高斯模糊、屏幕混合生成 Bloom 光晕，无需额外模组或光影包。泛光只作用于法阵，不会让整个画面一起泛白；实体遮挡仍参与发光层的深度判断。此效果不放置光源方块，不改变地形，结束或退出世界时释放渲染缓冲。

最终打击在动画约 116.8 秒时执行一次，作用于法阵所在维度中所有已加载的生物。普通生物直接移除，不经过伤害或死亡流程，不产生其死亡掉落和经验；非旁观者玩家先清空背包（含护甲、副手、光标物品及关闭合成界面后返回的材料），再走原生死亡流程，最后断开连接。旁观者玩家完全不受影响，创造模式玩家也在处理范围内。未加载的区块、其他维度、末影箱及容器存储不处理；方块和已有的掉落物不清除。打击前取消法阵会取消这次清除。

不修改时间、天气、玩家视角或移动。包含用户提供的原版《风雪史诗》配乐和参考视频的同步字幕。配乐与动画沿用原插件的统一时间轴，从第 0 秒同时开始，不含视频内的游戏声音。播放时暂停原版背景音乐的选择，结束后恢复原生音乐调度。音乐音量由游戏「音乐」滑块控制；取消、结束或离开世界时停止播放。中途进入可从当前动画时间播放。字幕使用参考视频的十段歌词、希腊字符解码效果和结尾字幕，与法阵共用时间轴。

在同一个创造栏可获取「达摩克里斯之剑」，也可执行：

```mcfunction
/give @s gocstrike:damocles_sword
```

右键沿准星方向投出剑。剑遇到实心方块或飞行超距会消失，命中生物才会锁定目标并开始播放《The Opening》。潜行右键可取消。同一玩家在同一维度再次施放时，上一场效果会解除。物品可重复使用。

- 命中后，目标沿锁定轴线升空，GOC 徽记始终跟在它脚下，一边自转一边展开，在目标停稳时达到完整尺寸。在有天花板的地方，目标会停在可用高度。目标提前死亡或离开时，演出会在原位置播完。
- 目标停稳后，头顶的法阵依照「奇术打击」原动画的顺序逐层展开。白色物质在法阵中心凝聚，随后沿上升路径从剑尖到剑柄逐段铸出约 62 格长的巨剑。巨剑完整显现后，法阵再升过剑柄一段距离。
- 巨剑小幅浮动并旋转五圈，充能让剑柄至剑尖依次变得更亮。充能结束时停转、扩散光环，随后短促上提，并在音乐第 70 秒下刺。
- 最终打击只抹除被锁定的生物，沿用上方所述的抹除规则。下刺末段拖出短残影，命中时在剑尖闪光并同时播放晶裂和重击，此时配乐短暂压低音量。剑尖停顿约 0.27 秒，双层冲击环从接触点扩散，法阵先震开，剑和徽记随后分解。音乐在第 78 秒结束。多人重叠施放时，附近观众收听最近一场达摩克里斯配乐。

### Forge 1.7.10

两件物品位于创造模式的「工具」栏。使用 `/give` 时，把 `@s` 替换为玩家名，物品 ID 相同。提示显示在聊天栏。该版本没有副手和旁观者模式，达摩克里斯之剑落下时用玻璃破碎声替代紫水晶音效。法阵的模糊泛光需要 OpenGL 3.0 和游戏的帧缓冲支持，条件不足时仍会绘制发光法阵线条。

### Forge 1.12.2

客户端和服务器都安装 `gocstrike-forge-1.12.2-<模组版本>.jar`，使用 Java 8 和相同的 Forge 版本。发布 JAR 已在 Forge 14.23.5.2864 服务端验证。两件物品位于创造模式的「工具」栏，也可以用上方的 `/give` 命令。两件物品都支持副手，旁观者免疫打击。

达摩克里斯之剑落下时用玻璃破碎声替代紫水晶音效。法阵的模糊泛光需要 OpenGL 3.0 和游戏帧缓冲支持，条件不足时仍会绘制发光线条。

## 构建

用 Java 25 运行 Gradle，Gradle 会从本地工具链选择各版本所需的 JDK。

```powershell
./gradlew.bat build                  # 构建根项目包含的版本
./gradlew.bat -p ports/forge-1.7.10 build  # 用本地 JDK 8 工具链独立构建 Forge 1.7.10
./gradlew.bat -p ports/forge-1.12.2 build  # 用本地 JDK 8 工具链独立构建 Forge 1.12.2
./gradlew.bat :build                 # 只构建 NeoForge 1.21.1
./gradlew.bat :forge-1.20.1:build    # 只构建 Forge 1.20.1
./gradlew.bat :neoforge-26.3:build   # 只构建 NeoForge 26.3
```

如果 Gradle 未自动发现已安装的 JDK，可用 `-Porg.gradle.java.installations.paths` 传入以逗号分隔的 JDK 安装目录。

发布 JAR 统一命名为 `gocstrike-<加载器>-<Minecraft 版本>-<模组版本>.jar`。NeoForge 1.21.1 的 `gocstrike-neoforge-1.21.1-<模组版本>.jar` 位于 `build/libs/`，其他版本的 JAR 位于 `ports/<加载器>-<Minecraft 版本>/build/libs/`。各版本均打包原有模型、动画和配乐。

Forge 1.7.10 和 1.12.2 使用 RetroFuturaGradle 独立构建。安装文件名形如 `gocstrike-forge-<Minecraft 版本>-<模组版本>.jar` 的 JAR。同目录下带 `-dev` 或 `-bundled-dev` 的文件用于开发。

## 动画来源与再生成

模型与动画来源于 SCPSL 游戏的[星河梦影](https://qm.qq.com/q/WYYBK75FWc)服务器。

模型和运动编排固定取自 `goc-strike-demo` 的提交：

```text
7f9a7c12f5c6576d129b6c5d7bae5a6336e66a2d
```

参考源文件和可重复生成步骤见 [tools/README.md](tools/README.md)。

## 许可与署名

- 软件代码： [MIT License](LICENSE)。NeoForge MDK 模板原有声明保留在 [TEMPLATE_LICENSE.txt](TEMPLATE_LICENSE.txt)。
- 法阵模型及由模型生成的资源：**All Rights Reserved（ARR，保留所有权利）**，不适用 MIT；具体范围见 [MODEL_LICENSE.txt](MODEL_LICENSE.txt)。
- 音乐：《风雪史诗》，作者：**西蒙Leonardo**。《The Opening》，音轨元数据署名：**塞壬唱片-MSR**。音乐不在本项目 MIT 授权范围内。
