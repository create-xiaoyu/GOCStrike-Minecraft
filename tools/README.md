# 动画资源生成

`reference/GocEmplacementMotion.cs` 和 `reference/goc-emplacement.mer.json` 是原插件提交 `7f9a7c12f5c6576d129b6c5d7bae5a6336e66a2d` 的逐字副本。源仓库为用户提供的本地 `goc-strike-demo`；这里仅使用模型和纯运动采样函数，不依赖或编译 LabAPI 插件。

- 运动源文件 SHA-256：`086cf039b1c6761e9a35fd9a47c620b69eec2a60c3621ed55ca571e8eb313a78`
- 模型源文件 SHA-256：`19d93cb025da1d55c2e699f278d2cc7014c3ce32c04f31b18c550831e933d65c`

在项目根目录执行（替换源仓库和临时目录路径）：

```powershell
python tools/prepare_animation.py --source 'C:/path/to/goc-strike-demo' --temp 'C:/Users/abc18/Desktop/Project/Java/Minecraft/.temp/gocstrike-reference'
```

将临时目录的 `bake-command.cs` 完整交给 Unity MCP 的 `Unity_RunCommand`。它使用 Unity 原生数学函数和基础网格，在 20 Hz 下采样原始 50 组姿态、逐笔轨道与颜色变化，并删除连续不变的采样。记录保持段最后一帧，防止插值在显现前提前启动。仅创建并立即移除三个临时网格对象，不修改 Unity 资产或原场景。

运行后执行：

```powershell
python tools/prepare_animation.py --compress
```

然后删除该次生成的临时输入和 C# 命令。压缩包位于 `src/main/resources/assets/gocstrike/animation/emplacement.bin.gz`，包含 3296 个层级节点、63919 个姿态关键帧，约 764 KiB。

文件为小端：魔数、版本、时长、采样率、三角网格表、节点表。节点记录父索引、网格编号和关键帧数组；每帧包括帧号、位置 3、四元数 4、缩放 3、颜色 4。Unity HDR 颜色归一化后转成 Minecraft 加色渲染颜色；保留色相及透明度。客户端以四元数球面插值处理旋转，由父节点累乘局部变换。发光使用无光照加色材质和几何光晕，没有 Unity 的实时灯光和屏幕后处理。

`strike-icon.svg` 是物品图标矢量源文件，通过 GIMP 导出为透明 PNG。
