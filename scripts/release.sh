#!/usr/bin/env bash
#
# scripts/release.sh — atlas-richie-ai 发布脚本
#
# 封装发布到 CNB 私服（SNAPSHOT 测试）和 Maven Central（release 生产）的完整流程，
# 替代 IDE 内置打包器无法传命令的限制。
#
# 用法：
#   ./scripts/release.sh snapshot    部署 SNAPSHOT 到 CNB 私服
#   ./scripts/release.sh release     部署 release 到 Maven Central
#   ./scripts/release.sh dry-run     只跑 verify，不 deploy（演练）
#
# 前置条件：
#   - Maven 4.0.0-rc5+
#   - JDK 17+
#   - ~/.m2/settings.xml 配好三组 server 凭据：
#       <id>richie696-repo-richie-snapshot</id>  → CNB token
#       <id>richie696-repo-richie-release</id>   → CNB token
#       <id>central</id>                         → Sonatype Central Portal token
#   - release 模式需要系统装好 gpg 并配 ~/.m2/settings.xml 的 gpg profile
#
# 设计原则：
#   - SNAPSHOT → CNB：签不签名都行（CNB 不验签），用 -Dgpg.skip=true 兼容无 gpg 环境
#   - release  → Central：强制签名（GPG 是 Central 的硬性要求）
#   - 默认行为贴近官方 mvn 命令，但把"容易忘的开关"固化进脚本

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

MODE="${1:-}"

# ---------------------------------------------------------------------
# 前置检查
# ---------------------------------------------------------------------
require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "[ERROR] $1 not found in PATH. $2" >&2
        exit 1
    fi
}

# 选 Maven：本项目已回退到 Maven 3 写法（modelVersion 4.0.0 + 硬编码 <version>1.0.0-SNAPSHOT</version>）。
# 优先 mvn3（Maven 3.9.x，wagon 稳定，CNB 部署无 JdkTransporter auth bug），
# fallback mvn（系统 PATH 上的 Maven，默认是 4.0.0-rc-5）。
MVN_CMD=""

select_maven() {
    if command -v mvn3 >/dev/null 2>&1; then
        MVN_CMD="mvn3"
        return
    fi
    if command -v mvn >/dev/null 2>&1; then
        MVN_CMD="mvn"
        return
    fi
    echo "[ERROR] mvn not found in PATH. Install Maven 3.9+ or 4.0+." >&2
    exit 1
}

check_mvn_version() {
    local ver
    ver="$($MVN_CMD --version 2>/dev/null | head -1 | awk '{print $3}')"
    if [[ ! "$ver" =~ ^3\.[89]\. ]] && [[ ! "$ver" =~ ^4\. ]]; then
        echo "[WARN] Maven $ver detected. This project supports Maven 3.9+ or 4.0+." >&2
    fi
}

# ---------------------------------------------------------------------
# 模式分发
# ---------------------------------------------------------------------
print_usage() {
    cat <<EOF
Usage: $0 <mode>

Modes:
  snapshot    Deploy SNAPSHOT to CNB private server (no GPG)
  release     Deploy release to Maven Central (GPG-signed)
  dry-run     Run verify only, no deploy
EOF
}

case "$MODE" in
    snapshot)
        echo "==> [snapshot] Build + Deploy SNAPSHOT to CNB"
        echo "    Target : https://maven.cnb.cool/richie696/repo-richie-snapshot/"
        echo "    GPG    : SKIPPED (-Dgpg.skip=true, since CNB doesn't need signed SNAPSHOTs)"
        echo ""
        require_command mvn3 "Install Maven 3.9.x (see CONTRIBUTING.md) or rename your mvn to mvn3"
        select_maven
        check_mvn_version
        $MVN_CMD deploy -DskipTests -Dgpg.skip=true
        echo ""
        echo "==> [snapshot] DONE. Verify on https://maven.cnb.cool/richie696/repo-richie-snapshot/-/packages/"
        ;;

    release)
        echo "==> [release] Build + Deploy to Maven Central"
        echo "    Target : Maven Central (via central-publishing-maven-plugin)"
        echo "    GPG    : REQUIRED (uses key from ~/.m2/settings.xml)"
        echo "    -Dmaven.deploy.skip=true  : prevents maven-deploy-plugin from also"
        echo "                              pushing to CNB release (only Central receives)"
        echo ""
        require_command mvn3 "Install Maven 3.9.x (see CONTRIBUTING.md)"
        require_command gpg "Install gnupg: apt-get install gnupg / brew install gnupg"
        select_maven
        check_mvn_version
        $MVN_CMD deploy -DskipTests -Dmaven.deploy.skip=true
        echo ""
        echo "==> [release] DONE. Track on https://central.sonatype.com/publisher/deployments"
        ;;

    dry-run)
        echo "==> [dry-run] Running verify only (no deploy)"
        echo "    -Dgpg.skip=true  Skip GPG signing — local dev machines usually"
        echo "                   don't have the release GPG key configured."
        echo ""
        select_maven
        check_mvn_version
        $MVN_CMD -DskipTests -Dgpg.skip=true clean verify
        echo ""
        echo "==> [dry-run] DONE. No artifacts published."
        ;;

    *)
        print_usage
        exit 1
        ;;
esac