Project address: http://154.12.28.56:8080/

提交PR方法：

步骤 1：Fork 仓库

在你的仓库页面，点击右上角 Fork → 在协作者自己的 GitHub 账号下生成一份副本。

步骤 2：克隆 Fork 到本地
git clonehttps://github.com/KaizhengWang/climateMonitor.git

cd 项目

步骤 3：创建分支开发
git checkout -b feature/xxx

feature/xxx 可替换成功能名，比如 feature/login。

步骤 4：修改、提交代码
git add .
git commit -m "实现 xxx 功能"

步骤 5：推送分支到协作者自己的 GitHub 仓库
git push origin feature/xxx

步骤 6：在 GitHub 上创建 Pull Request

打开协作者仓库页面 → 选择刚推送的 feature/xxx 分支。

点击 Pull Request → New Pull Request → base: main, compare: feature/xxx。

填写标题和描述 → 提交 PR。