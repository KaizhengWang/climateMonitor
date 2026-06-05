Here's the English translation of the "How to Submit a Pull Request (PR)" guide:

How to Submit a PR:

Step 1: Fork the repository

On the repository page, click Fork in the upper right corner → this creates a copy under the contributor's own GitHub account.

Step 2: Clone the forked repository locally

bash
git clone https://github.com/KaizhengWang/climateMonitor.git
cd project
Step 3: Create a branch for development

bash
git checkout -b feature/xxx
Replace feature/xxx with a feature name, e.g., feature/login.

Step 4: Make changes and commit the code

bash
git add .
git commit -m "Implement xxx feature"
Step 5: Push the branch to the contributor's own GitHub repository

bash
git push origin feature/xxx
Step 6: Create a Pull Request on GitHub

Open the contributor's repository page → select the branch you just pushed (feature/xxx).

Click Pull Request → New Pull Request → set base: main, compare: feature/xxx.

Fill in the title and description → submit the PR.

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