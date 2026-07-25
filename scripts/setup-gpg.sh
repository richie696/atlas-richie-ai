#!/usr/bin/env bash
#
# scripts/setup-gpg.sh — atlas-richie-ai 一次性 GPG 初始化
#
# 在新机器（开发机或 release CI runner）首次发布 release 前运行一次。
# 脚本会：
#   1. 检查 gpg 是否安装（macOS / Linux 跨平台）
#   2. 生成 GPG key（如果还没有）
#   3. 把公钥上传到 keys.openpgp.org 公钥服务器
#   4. 打印 ~/.m2/settings.xml 里需要添加的 profile 片段
#
# 用法：
#   ./scripts/setup-gpg.sh
#
# 注意事项：
#   - 运行中会提示输入姓名 / 邮箱 / 密码，请使用真实信息
#   - Sonatype Central Portal 的 contact email 必须和 key 的邮箱一致
#   - 同一个 key 只生成一次，备份好私钥（~/.gnupg/）

set -euo pipefail

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "[ERROR] $1 not found. $2" >&2
        exit 1
    fi
}

# ---------------------------------------------------------------------
# 1. 检查 gpg
# ---------------------------------------------------------------------
require_command gpg "Install with: apt-get install gnupg (Debian/Ubuntu/CNB) | brew install gnupg (macOS)"

# ---------------------------------------------------------------------
# 2. 检查是否已有 key
# ---------------------------------------------------------------------
echo "==> Checking existing GPG keys..."
EXISTING_KEYS=$(gpg --list-secret-keys --with-colons 2>/dev/null | awk -F: '/^sec/ {print $5}' | head -1 || true)

if [[ -n "$EXISTING_KEYS" ]]; then
    echo "    Found existing key: $EXISTING_KEYS"
    KEY_ID="$EXISTING_KEYS"
    read -r -p "Use this key? [Y/n] " USE_EXISTING
    if [[ "$USE_EXISTING" =~ ^[Nn]$ ]]; then
        KEY_ID=""
    fi
fi

if [[ -z "${KEY_ID:-}" ]]; then
    echo "==> No existing key. Generating a new one..."
    echo "    You'll be prompted for:"
    echo "      - Real name    : your full name"
    echo "      - Email        : contact email registered with Sonatype"
    echo "      - Passphrase   : used as <gpg.passphrase> in settings.xml"
    echo ""
    # --batch 模式下生成需要先准备参数；这里走交互式 gen-key
    gpg --gen-key
    KEY_ID=$(gpg --list-secret-keys --with-colons 2>/dev/null | awk -F: '/^sec/ {print $5}' | head -1)
fi

if [[ -z "$KEY_ID" ]]; then
    echo "[ERROR] No key ID detected. Aborting." >&2
    exit 1
fi

echo ""
echo "==> Using key ID: $KEY_ID"

# ---------------------------------------------------------------------
# 3. 上传公钥到公钥服务器
# ---------------------------------------------------------------------
echo "==> Uploading public key to keys.openpgp.org ..."
gpg --keyserver keys.openpgp.org --send-keys "$KEY_ID"
echo "    Uploaded."

# 备用：hkp://pgp.mit.edu 也是常见服务器，但 keys.openpgp.org 是 Sonatype 推荐的
# gpg --keyserver hkp://pgp.mit.edu --send-keys "$KEY_ID"

echo ""
echo "============================================================"
echo "Next steps"
echo "============================================================"
echo ""
echo "1. 把以下片段追加到 ~/.m2/settings.xml："
echo ""
cat <<EOF
  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.keyname>${KEY_ID}</gpg.keyname>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
EOF
echo ""
echo "   ⚠️  把 YOUR_GPG_PASSPHRASE 换成你刚才 gen-key 时设置的密码。"
echo "   ⚠️  如果不想明文存 passphrase，可以考虑 Maven 凭据加密（mvn --encrypt-master-password）。"
echo ""
echo "2. 发布 release："
echo "   ./scripts/release.sh release"
echo ""
echo "3. 备份 ~/.gnupg/ 目录 —— 私钥丢了就没法再签。"

echo ""
echo "==> setup-gpg.sh DONE."