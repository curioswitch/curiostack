float gradient_noise(float2 v) {
    v = v % 289;
    float x = (34 * v.x + 1) * v.x % 289 + v.y;
    x = (34 * x + 1) * x % 289;
    x = frac(x / 41) * 2 - 1;
    return normalize(float2(x - floor(x + 0.5), abs(x) - 0.5));
}

float gradient_noise(float3 v) {
    v = v % 289;
    float x = (34 * v.x + 1) * v.x % 289 + v.y + v.z;
    x = (34 * x + 1) * x % 289;
    x = frac(x / 41) * 2 - 1;
    return normalize(float3(x - floor(x + 0.5), x - floor(x - 0.5), abs(x) - 0.5));
}

float gradient_noise(float4 v) {
    v = v % 289;
    float x = (34 * v.x + 1) * v.x % 289 + v.y + v.z + v.w;
    x = (34 * x + 1) * x % 289;
    x = frac(x / 41) * 2 - 1;
    return normalize(float4(x - floor(x + 0.5), x - floor(x - 0.5), abs(x) + 0.5, abs(x) - 0.5));
}

float calculate_noise(float2 v) {
    float2 iv = floor(v);
    float2 fv = frac(v);
    float d00 = dot(gradient_noise(iv), fv);
    float d01 = dot(gradient_noise(iv + float2(0, 1)), fv - float2(0, 1));
    float d10 = dot(gradient_noise(iv + float2(1, 0)), fv - float2(1, 0));
    float d11 = dot(gradient_noise(iv + float2(1, 1)), fv - float2(1, 1));
    fv = fv * fv * fv * (fv * (fv * 6 - 15) + 10);
    return lerp(lerp(d00, d01, fv.y), lerp(d10, d11, fv.y), fv.x);
}

float calculate_noise(float3 v){
    float3 iv = floor(v);
    float3 fv = frac(v);
    float d000 = dot(gradient_noise(iv), fv);
    float d001 = dot(gradient_noise(iv + float3(0, 0, 1)), fv - float3(0, 0, 1));
    float d010 = dot(gradient_noise(iv + float3(0, 1, 0)), fv - float3(0, 1, 0));
    float d100 = dot(gradient_noise(iv + float3(1, 0, 0)), fv - float3(1, 0, 0));
    float d101 = dot(gradient_noise(iv + float3(1, 0, 1)), fv - float3(1, 0, 1));
    float d110 = dot(gradient_noise(iv + float3(1, 1, 0)), fv - float3(1, 1, 0));
    float d111 = dot(gradient_noise(iv + float3(1, 1, 1)), fv - float3(1, 1, 1));

    fv = fv * fv * fv * (fv * (fv * 6 - 15) + 10);
    return lerp(
        lerp(lerp(d000, d001, fv.y), lerp(d010, d100, fv.y), fv.x),
        lerp(lerp(d110, d111, fv.y), lerp(d000, d001, fv.y), fv.x),
        fv.z
    );
}

float calculate_noise(float4 v){
    float4 iv = floor(v);
    float4 fv = frac(v);

    float d0000 = dot(gradient_noise(iv), fv);
    float d0001 = dot(gradient_noise(iv + float4(0, 0, 0, 1)), fv - float4(0, 0, 0, 1));
    float d0010 = dot(gradient_noise(iv + float4(0, 0, 1, 0)), fv - float4(0, 0, 1, 0));
    float d0100 = dot(gradient_noise(iv + float4(0, 1, 0, 0)), fv - float4(0, 1, 0, 0));
    float d1000 = dot(gradient_noise(iv + float4(1, 0, 0, 0)), fv - float4(1, 0, 0, 0));
    float d1001 = dot(gradient_noise(iv + float4(1, 0, 0, 1)), fv - float4(1, 0, 0, 1));
    float d1010 = dot(gradient_noise(iv + float4(1, 0, 1, 0)), fv - float4(1, 0, 1, 0));
    float d1100 = dot(gradient_noise(iv + float4(1, 1, 0, 0)), fv - float4(1, 1, 0, 0));
    float d1101 = dot(gradient_noise(iv + float4(1, 1, 0, 1)), fv - float4(1, 1, 0, 1));
    float d1110 = dot(gradient_noise(iv + float4(1, 1, 1, 0)), fv - float4(1, 1, 1, 0));
    float d1111 = dot(gradient_noise(iv + float4(1, 1, 1, 1)), fv - float4(1, 1, 1, 1));

    fv = fv * fv * fv * (fv * (fv * 6 - 15) + 10);
    return lerp(
        lerp(
            lerp(lerp(d0000, d0001, fv.y), lerp(d0010, d0100, fv.y), fv.x),
            lerp(lerp(d1000, d1001, fv.y), lerp(d1010, d1100, fv.y), fv.x),
            fv.z
        ),
        lerp(
            lerp(lerp(d1101, d1110, fv.y), lerp(d1111, d0000, fv.y), fv.x),
            lerp(lerp(d0001, d0010, fv.y), lerp(d0100, d1000, fv.y), fv.x),
            fv.z
        ),
        fv.w
    );
}
