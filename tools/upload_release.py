#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
把 dist 里的 APK 和 version.json 传到 GitHub Release。

用法：
  python upload_release.py --repo 你的用户名/shanganba --tag v1.1.0 --token ghp_xxx
  python upload_release.py --repo 你的用户名/shanganba --tag v1.1.0 --token ghp_xxx --create-repo
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request

API = "https://api.github.com"
UA = "shanganba-release/1.0"


def request(method, url, token, data=None, content_type="application/json", raw=None):
    headers = {
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "User-Agent": UA,
        "X-GitHub-Api-Version": "2022-11-28",
    }
    body = None
    if raw is not None:
        body = raw
        headers["Content-Type"] = content_type
    elif data is not None:
        body = json.dumps(data).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=180) as resp:
            text = resp.read().decode("utf-8", "replace")
            return resp.status, (json.loads(text) if text.strip().startswith(("{", "[")) else text)
    except urllib.error.HTTPError as e:
        detail = e.read().decode("utf-8", "replace")
        return e.code, detail


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--repo", required=True, help="owner/name")
    ap.add_argument("--tag", default="v1.1.0")
    ap.add_argument("--token", default="", help="不给就从 tools/upload-token.txt 读，避免 token 出现在聊天或命令历史里")
    ap.add_argument("--dist", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "dist"))
    ap.add_argument("--create-repo", action="store_true", help="仓库不存在时自动新建")
    ap.add_argument("--private", action="store_true", help="新建仓库时设为私有")
    ap.add_argument("--make-public", action="store_true", help="仓库是私有时改成公开（App 才能免鉴权下载）")
    ap.add_argument("--notes", default="", help="Release 说明，留空则用 version.json 里的 notes")
    args = ap.parse_args()

    token = args.token.strip()
    if not token:
        token_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "upload-token.txt")
        if os.path.exists(token_file):
            with open(token_file, "r", encoding="utf-8-sig") as fh:
                for line in fh:
                    line = line.strip()
                    if line and not line.startswith("#"):
                        token = line
                        break
        if not token:
            print("没有 token：请在 %s 里写入你的 GitHub token（只要一行），" % token_file)
            print("或者运行命令时加 --token 参数。")
            return 1
    args.token = token

    dist = os.path.abspath(args.dist)
    candidates = [f for f in os.listdir(dist) if f.endswith(".apk")]
    if not candidates:
        print("dist 里没有 APK")
        return 1
    apk_name = max(candidates, key=lambda f: os.path.getmtime(os.path.join(dist, f)))
    apk_path = os.path.join(dist, apk_name)

    status, user = request("GET", API + "/user", args.token)
    if status != 200:
        print("token 校验失败（%s）：%s" % (status, user))
        print("请确认 token 有效，且勾选了 repo / Contents 读写权限。")
        return 1
    owner_login = user["login"]
    print("已登录：%s" % owner_login)

    repo = args.repo
    status, info = request("GET", API + "/repos/" + repo, args.token)
    if status == 404 and args.create_repo:
        name = repo.split("/")[-1]
        status, info = request("POST", API + "/user/repos", args.token,
                               {"name": name, "private": bool(args.private),
                                "description": "上岸吧 · 安徽省考备考 App 自用发布仓库",
                                "auto_init": False})
        if status not in (200, 201):
            print("新建仓库失败（%s）：%s" % (status, info))
            return 1
        print("已新建仓库：%s" % info["full_name"])
    elif status != 200:
        print("仓库读不到（%s）：%s" % (status, info))
        return 1
    else:
        print("使用已有仓库：%s" % info["full_name"])

    if info.get("private") and args.make_public:
        print("仓库当前是私有，正在改成公开 …")
        status, changed = request("PATCH", API + "/repos/" + repo, args.token,
                                  {"private": False, "visibility": "public"})
        if status != 200:
            print("改成公开失败（%s）：%s" % (status, changed))
            print("请确认 token 勾选了 repo 权限，并且你对这个仓库有管理员权限。")
            return 1
        print("已改成公开：%s" % ("public" if not changed.get("private") else "仍然是私有"))
    elif info.get("private"):
        print("注意：仓库还是私有，手机端将无法下载更新包。")

    # 已存在同名 tag 的 release 就先删掉，保证重复执行不报错
    status, release = request("GET", "%s/repos/%s/releases/tags/%s" % (API, repo, args.tag), args.token)
    if status == 200:
        print("发现已存在的 %s，先删除旧 release" % args.tag)
        request("DELETE", "%s/repos/%s/releases/%d" % (API, repo, release["id"]), args.token)
        request("DELETE", "%s/repos/%s/git/refs/tags/%s" % (API, repo, args.tag), args.token)

    version_path = os.path.join(dist, "version.json")
    with open(version_path, "r", encoding="utf-8") as fh:
        manifest = json.load(fh)
    notes = args.notes or manifest.get("notes") or ("上岸吧 " + os.path.basename(apk_path))
    print("APK：%s（%.2f MB）" % (apk_name, os.path.getsize(apk_path) / 1048576.0))
    print("创建 release %s …" % args.tag)
    status, release = request("POST", "%s/repos/%s/releases" % (API, repo), args.token, {
        "tag_name": args.tag,
        "name": "上岸吧 " + args.tag.lstrip("v"),
        "body": notes,
        "draft": False,
        "prerelease": False,
    })
    if status not in (200, 201):
        print("创建 release 失败（%s）：%s" % (status, release))
        return 1

    upload_url = release["upload_url"].split("{")[0]

    print("上传 %s …" % apk_name)
    with open(apk_path, "rb") as fh:
        apk_bytes = fh.read()
    status, asset = request(
        "POST", "%s?name=%s" % (upload_url, apk_name), args.token,
        raw=apk_bytes, content_type="application/vnd.android.package-archive"
    )
    if status not in (200, 201):
        print("APK 上传失败（%s）：%s" % (status, asset))
        return 1
    apk_url = asset["browser_download_url"]
    print("APK 直链：%s" % apk_url)

    # 生成带直链的 version.json，同时覆盖本地文件
    manifest["url"] = apk_url
    manifest["notes"] = notes
    with open(version_path, "w", encoding="utf-8") as fh:
        json.dump(manifest, fh, ensure_ascii=False, indent=2)

    print("上传 version.json …")
    with open(version_path, "rb") as fh:
        manifest_bytes = fh.read()
    status, asset2 = request(
        "POST", "%s?name=version.json" % upload_url, args.token,
        raw=manifest_bytes, content_type="application/json"
    )
    if status not in (200, 201):
        print("version.json 上传失败（%s）：%s" % (status, asset2))
        return 1
    manifest_url = asset2["browser_download_url"]

    print("")
    print("=== 完成 ===")
    print("Release 页面：%s" % release["html_url"])
    print("APK 直链    ：%s" % apk_url)
    print("更新地址    ：%s" % manifest_url)
    print("")
    print("把上面「更新地址」填进 App 的：我的 → 应用内更新 → 更新地址")
    return 0


if __name__ == "__main__":
    sys.exit(main())
