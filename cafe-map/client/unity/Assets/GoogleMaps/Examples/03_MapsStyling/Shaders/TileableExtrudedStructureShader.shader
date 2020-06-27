Shader "Custom/TileableExtrudedStructureShader" {
  Properties {
    _Color ("Color", Color) = (1,1,1,1)

    _MainTex ("Albedo (RGB)", 2D) = "white" {}
    _BumpMap ("Bumpmap", 2D) = "bump" {}

    _WallSecondaryTex ("Seconday Wall Albedo (RGB)", 2D) = "white" {}
    _WallSecondaryBumpMap ("Secondary Bumpmap", 2D) = "bump" {}

    _WallThirdTex ("Third Albedo (RGB)", 2D) = "white" {}
    _WallThirdBumpMap ("Third Bumpmap", 2D) = "bump" {}

    _RoofTex ("Roof Albedo (RGB)", 2D) = "white" {}
    _RoofBumpMap ("Roof Bumpmap", 2D) = "bump" {}
    _BottomTex ("BottomAlbedo (RGB)", 2D) = "white" {}
    _BottomBumpMap ("BottomBumpmap", 2D) = "bump" {}
    _BottomWindowTex ("BottomWindowAlbedo (RGB)", 2D) = "white" {}
    _BottomWindowBumpMap ("BottomWindowBumpmap", 2D) = "bump" {}
    _Glossiness ("Smoothness", Range(0,1)) = 0.5
    _Metallic ("Metallic", Range(0,1)) = 0.0
    _FloorThreshold("_Floor Threshold", float)  = 0.6
    _DisplacementAmount("_Displacement Amount", float)  = 1.0
    _GeometryHeight("_Geometry Height", float)  = 0.0
    _RoofParapet("_Roof Parapet Height", float)  = 0.0
  }
  SubShader {
    Tags {
      "RenderType" = "Opaque"
    }
    LOD 200

    CGPROGRAM
    // Physically based Standard lighting model, and enable shadows on all light types
    #pragma surface surf Standard fullforwardshadows vertex:vert addshadow

    // Use shader model 3.0 target, to get nicer looking lighting
    #pragma target 3.0

    sampler2D _MainTex;
    sampler2D _BottomTex;
    sampler2D _BottomWindowTex;
    sampler2D _BumpMap;
    sampler2D _BottomBumpMap;
    sampler2D _BottomWindowBumpMap;

    sampler2D _WallSecondaryTex;
    sampler2D _WallSecondaryBumpMap;

    sampler2D _WallThirdTex;
    sampler2D _WallThirdBumpMap;

    sampler2D _RoofTex;
    sampler2D _RoofBumpMap;

    float _FloorThreshold;
    float _DisplacementAmount;
    float _GeometryHeight;
    float _RoofParapet;

    struct Input {
      float2 uv_MainTex;
      float2 uv_BottomTex;
      float2 uv_BumpMap;
      float3 worldPos;
      float4 vertexColor;
    };

    struct appdata {
      float4 vertex : POSITION;
      float3 normal: NORMAL;
      float4 tangent: TANGENT;
      float4 texcoord : TEXCOORD0;
      float4 texcoord1 : TEXCOORD1;
      float4 texcoord2 : TEXCOORD2;
    };

    void vert(inout appdata v, out Input o) {
      UNITY_INITIALIZE_OUTPUT(Input,o);

      float4 pos = v.vertex;
      if (pos.y > 10.0){
        o.vertexColor = float4(0.3, 0.3, 0.3, 1.0);
      } else {
        o.vertexColor = float4(1.0, 1.0, 1.0, 1.0);
      }
    }

    half _Glossiness;
    half _Metallic;
    fixed4 _Color;

    // Add instancing support for this shader.
    // 'Enable Instancing' must be enabled on materials that use this shader.
    // See https://docs.unity3d.com/Manual/GPUInstancing.html for more information about instancing.
    // #pragma instancing_options assumeuniformscaling
    UNITY_INSTANCING_BUFFER_START(Props)
    // TODO: Put more per-instance properties here.
    UNITY_INSTANCING_BUFFER_END(Props)

    void surf(Input IN, inout SurfaceOutputStandard o) {
      float4 color = float4(0.0, 0.0, 0.0, 0.0);
      float metalness = _Metallic;

      float normalScale = 2.0;
      if (IN.worldPos.y > _FloorThreshold) {
        float tiledWallY = fmod((IN.worldPos.y) * 0.1, 3.0);

        float2 uv = IN.uv_MainTex;
        float2 uvNormal = IN.uv_BumpMap;
        uv.y = tiledWallY;
        uvNormal.y = tiledWallY;

        float m = floor(tiledWallY);
        if (m == 0.0) {
          color = tex2D(_MainTex, uv ) * _Color * IN.vertexColor;
          o.Normal = UnpackScaleNormal(tex2D(_BumpMap, uvNormal), normalScale);
        } else if (m == 1.0) {
          color = tex2D (_WallSecondaryTex, uv) * _Color * IN.vertexColor;
          o.Normal = UnpackScaleNormal(tex2D(_WallSecondaryBumpMap, uvNormal), normalScale);
        } else if (m == 2.0) {
          color = tex2D(_WallThirdTex, uv) * _Color * IN.vertexColor;
          o.Normal = UnpackScaleNormal(tex2D( _WallThirdBumpMap, uvNormal), normalScale);
        }
      } else {
        float2 scaledDoorUV = float2(IN.uv_BottomTex.x, IN.worldPos.y / _FloorThreshold);

        if (IN.uv_BottomTex.x < 1.0){
          color = tex2D( _BottomTex, scaledDoorUV ) * _Color;
          o.Normal = UnpackScaleNormal(tex2D(_BottomBumpMap, scaledDoorUV), normalScale);
          metalness = color.a;
        } else {
          color = tex2D( _BottomWindowTex, scaledDoorUV ) * _Color;
          o.Normal = UnpackScaleNormal(tex2D(_BottomWindowBumpMap, scaledDoorUV), normalScale);
        }
      }

      o.Albedo = color.rgb;
      o.Metallic = _Metallic;
      o.Smoothness = _Glossiness;
      o.Alpha = 1.0;
    }
    ENDCG
  }
  FallBack "Diffuse"
}
