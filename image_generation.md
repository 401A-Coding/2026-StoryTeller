> ## Documentation Index
> Fetch the complete documentation index at: https://platform.minimaxi.com/docs/llms.txt
> Use this file to discover all available pages before exploring further.

# 图片生成

> 图片生成服务提供文生图（text-to-image）与图生图（image-to-image）两种核心功能。

## 根据文本生成图片

根据详尽的文本描述（prompt），直接生成与之匹配的图片。

<CodeGroup>
  ```python 请求示例 theme={null}
  import base64
  import requests
  import os

url = "https://api.minimaxi.com/v1/image_generation"
api_key = os.environ.get("MINIMAX_API_KEY")
headers = {"Authorization": f"Bearer {api_key}"}

payload = {
"model": "image-01",
"prompt": "men Dressing in white t shirt, full-body stand front view image :25, outdoor, Venice beach sign, full-body image, Los Angeles, Fashion photography of 90s, documentary, Film grain, photorealistic",
"aspect_ratio": "16:9",
"response_format": "base64",
}

response = requests.post(url, headers=headers, json=payload)
response.raise_for_status()

images = response.json()["data"]["image_base64"]

for i in range(len(images)):
with open(f"output-{i}.jpeg", "wb") as f:
f.write(base64.b64decode(images[i]))
  ```
</CodeGroup>

生成结果：

![图片描述](https://filecdn.minimax.chat/public/5d969bc8-047c-424d-b7f8-87ece21e8f13.jpeg)

## 结合参考图生成图片

此功能允许提供一张或多张包含清晰主体的参考图（支持网络图片链接），并结合 prompt 描述，生成一张保留了主体特征的新图片。该功能尤其适用于需要保持人物形象一致性的场景，例如为同一个虚拟角色生成不同情境下的图片。

<CodeGroup>
  ```python 请求示例 theme={null}
  import base64
  import requests
  import os

  url = "https://api.minimaxi.com/v1/image_generation"
  api_key = os.environ.get("MINIMAX_API_KEY")
  headers = {"Authorization": f"Bearer {api_key}"}

  payload = {
      "model": "image-01",
      "prompt": "女孩在图书馆的窗户前，看向远方",
      "aspect_ratio": "16:9",
      "subject_reference": [
          {
              "type": "character",
              "image_file": "https://cdn.hailuoai.com/prod/2025-08-12-17/video_cover/1754990600020238321-411603868533342214-cover.jpg",
          }
      ],
      "response_format": "base64",
  }

  response = requests.post(url, headers=headers, json=payload)
  response.raise_for_status()
  images = response.json()["data"]["image_base64"]

  for i in range(len(images)):
      with open(f"output-{i}.jpeg", "wb") as f:
          f.write(base64.b64decode(images[i]))
  ```
</CodeGroup>

生成结果：

![图片描述](https://filecdn.minimax.chat/public/0a70cf90-9ae2-4593-b4a3-84ec9ab3429a.jpeg)

## 推荐阅读

<Columns cols={2}>
  <Card title="文生图接口" icon="book-open" href="/api-reference/image-generation-t2i" arrow="true" cta="点击查看">
    使用 API 接口，输入文本描述，进行图片生成。
  </Card>

  <Card title="图生图接口" icon="book-open" href="/api-reference/image-generation-i2i" arrow="true" cta="点击查看">
    使用 API 接口，输入图片内容，进行图片生成。
  </Card>

  <Card title="产品定价" icon="book-open" href="/guides/pricing-paygo#图像" arrow="true" cta="点击查看">
    各模型的定价说明、计费方式及使用限制。
  </Card>

  <Card title="速率限制" icon="book-open" href="/guides/rate-limits#3、我们的-api-的限速具体数值" arrow="true" cta="点击查看">
    为保证资源的高效使用，引入速率限制，以确保服务的可用性、稳定性。
  </Card>
</Columns>
