# Luokixi Server Channel

这是 LSI（Luokixi Server Installer）的公开稳定更新频道仓库。

## 当前状态

- 稳定频道：已上线
- 客户端版本：`1.0.0`
- Release 标签：`client-1.0.0`
- 客户端包：`luokixi-client-bootstrap-1.0.0.zip`
- LSI 最低版本：`0.8.0`

## 文件说明

- `stable.json`：LSI 正式更新清单
- `stable.example.json`：清单示例
- 大型客户端更新包通过 GitHub Releases 分发

LSI 会读取 `stable.json`，下载 Release 中的客户端包，进行 SHA256 校验、备份并部署到正确的 `.minecraft` 实例。
