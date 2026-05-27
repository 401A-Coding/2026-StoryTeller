> ## Documentation Index
> Fetch the complete documentation index at: https://platform.minimaxi.com/docs/llms.txt
> Use this file to discover all available pages before exploring further.

# 文生图

> 使用本接口，输出文本内容，进行图片生成。



## OpenAPI

````yaml /api-reference/image/generation/api/text-to-image.json POST /v1/image_generation
openapi: 3.1.0
info:
  title: MiniMax Image Generation API
  description: MiniMax image generation API for creating images from text prompts
  license:
    name: MIT
  version: 1.0.0
servers:
  - url: https://api.minimaxi.com
security:
  - bearerAuth: []
paths:
  /v1/image_generation:
    post:
      tags:
        - Image
      summary: Image Generation
      operationId: imageGeneration
      parameters:
        - name: Content-Type
          in: header
          required: true
          description: 请求体的媒介类型，请设置为 `application/json` 确保请求数据的格式为 JSON.
          schema:
            type: string
            enum:
              - application/json
            default: application/json
      requestBody:
        description: ''
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ImageGenerationReq'
        required: true
      responses:
        '200':
          description: ''
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ImageGenerationResp'
components:
  schemas:
    ImageGenerationReq:
      type: object
      required:
        - prompt
        - model
      properties:
        model:
          type: string
          description: 模型名称。可选值：`image-01`，`image-01-live`
          enum:
            - image-01
            - image-01-live
        prompt:
          type: string
          description: 图像的文本描述，最长 1500 字符
        style:
          $ref: '#/components/schemas/StyleObject'
          type: object
          description: 画风设置，仅当 `model` 为 `image-01-live` 时生效
        aspect_ratio:
          type: string
          description: |-
            图像宽高比，默认为 1:1。可选值：
            - `1:1` (1024x1024)
            - `16:9` (1280x720)
            - `4:3` (1152x864)
            - `3:2` (1248x832)
            - `2:3` (832x1248)
            - `3:4` (864x1152)
            - `9:16` (720x1280)
            - `21:9` (1344x576) (仅适用于`image-01`)
          enum:
            - '1:1'
            - '16:9'
            - '4:3'
            - '3:2'
            - '2:3'
            - '3:4'
            - '9:16'
            - '21:9'
        width:
          type: integer
          description: >-
            生成图片的宽度（像素）。仅当 model 为 `image-01` 时生效。注意：`width` 和 `height`
            需同时设置，取值范围[512, 2048]，且必须是 8 的倍数。若与 `aspect_ratio` 同时设置，则优先使用
            `aspect_ratio`
        height:
          type: integer
          description: >-
            生成图片的高度（像素）。仅当 model 为 `image-01` 时生效。注意：`width` 和 `height`
            需同时设置，取值范围[512, 2048]，且必须是 8 的倍数。若与 `aspect_ratio` 同时设置，则优先使用
            `aspect_ratio`
        response_format:
          type: string
          enum:
            - url
            - base64
          default: url
          description: |-
            返回图片的形式，默认为 url。可选值：url, base64。
            ⚠️ 注意：url 的有效期为 24 小时
        seed:
          type: integer
          format: int64
          description: 随机种子。使用相同的 seed 和参数，可以生成内容相近的图片，用于复现结果。如未提供，算法会对 n 张图单独生成随机种子
        'n':
          type: integer
          default: 1
          minimum: 1
          maximum: 9
          description: 单次请求生成的图片数量，取值范围[1, 9]，默认为 1
        prompt_optimizer:
          type: boolean
          default: false
          description: 是否开启 prompt 自动优化，默认为 `false`.
        aigc_watermark:
          type: boolean
          description: 是否在生成的图片中添加水印，默认为 `false`
      example:
        model: image-01
        prompt: >-
          A man in a white t-shirt, full-body, standing front view, outdoors,
          with the Venice Beach sign in the background, Los Angeles. Fashion
          photography in 90s documentary style, film grain, photorealistic.
        aspect_ratio: '16:9'
        response_format: url
        'n': 3
        prompt_optimizer: true
    ImageGenerationResp:
      type: object
      properties:
        data:
          $ref: '#/components/schemas/DataObject'
        metadata:
          type: object
          properties:
            success_count:
              type: integer
              description: N成功生成的图片数量
            failed_count:
              type: integer
              description: Number of images blocked due to content safety.
          description: 因内容安全检查失败而未返回的图片数量
        id:
          type: string
          description: 生成任务的 ID，用于后续查询任务状态
        base_resp:
          $ref: '#/components/schemas/BaseResp'
      example:
        id: 03ff3cd0820949eb8a410056b5f21d38
        data:
          image_urls:
            - XXX
            - XXX
            - XXX
        metadata:
          failed_count: '0'
          success_count: '3'
        base_resp:
          status_code: 0
          status_msg: success
    StyleObject:
      type: object
      properties:
        style_type:
          type: string
          required: true
          description: 画风风格类型。可选值：`漫画`, `元气`, `中世纪`, `水彩`
        style_weight:
          type: number
          format: float
          description: 画风权重，取值范围 `(0, 1]`，默认 `0.8`
    DataObject:
      type: object
      properties:
        image_urls:
          type: array
          items:
            type: string
          description: 当 `response_format` 为 `url` 时返回，包含图片链接的数组
        image_base64:
          type: array
          items:
            type: string
          description: 当 `response_format` 为 `base64` 时返回，包含图片 Base64 编码的数组
    BaseResp:
      type: object
      properties:
        status_code:
          type: integer
          description: |-
            状态码及其分别含义如下：
            - 0，请求成功
            - 1002，触发限流，请稍后再试
            - 1004，账号鉴权失败，请检查 API-Key 是否填写正确
            - 1008，账号余额不足
            - 1026，图片描述涉及敏感内容
            - 2013，传入参数异常，请检查入参是否按要求填写
            - 2049，无效的api key

            更多内容可查看[错误码查询列表](/api-reference/errorcode)了解详情  
        status_msg:
          type: string
          description: 具体错误详情
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |-
        `HTTP: Bearer Auth`
         - Security Scheme Type: http
         - HTTP Authorization Scheme: Bearer API_key，用于验证账户信息，可在 [账户管理>接口密钥](https://platform.minimaxi.com/user-center/basic-information/interface-key) 中查看。

````