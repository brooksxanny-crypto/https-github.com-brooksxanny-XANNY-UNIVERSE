package universe.xanny.app.render

/** GLES2 bounded volumetric raymarch. Depth-occluded GLES2 volume with OES depth texture; runtime verification pending. */
object VolumetricRaymarch {
    const val steps = 48
    const val vertexShader = """attribute vec2 aScreen; varying vec2 vUv;
        void main(){vUv=aScreen*0.5+0.5; gl_Position=vec4(aScreen,0.0,1.0);}"""
    const val fragmentShader = """precision highp float;
        varying vec2 vUv;
        uniform vec3 uEye, uRight, uUp, uForward;
        uniform float uAspect, uTanHalfFov, uTime;
        uniform sampler2D uDepth;
        uniform float uNear, uFar;
        uniform vec3 uLightDirection;
        uniform float uLightExtinction;
        uniform vec4 uShadowSpheres[3];
        uniform float uShadowCount;
        const int STEPS=48;
        float edge(float distanceToEdge) {
            float t=clamp((0.7-distanceToEdge)/0.7,0.0,1.0);
            return t*t*(3.0-2.0*t);
        }
        float density(vec3 p) {
            vec3 bounds=vec3(3.7,2.5,4.0);
            if(any(greaterThan(abs(p),bounds))) return 0.0;
            float left=edge(p.x+bounds.x), right=edge(bounds.x-p.x);
            float bottom=edge(p.y+bounds.y), top=edge(bounds.y-p.y);
            float sides=max(max(left,right),max(top,bottom));
            float corners=max(max(left*top,right*top),max(left*bottom,right*bottom));
            float depthEnvelope=0.35+0.65*(1.0-abs(p.z)/bounds.z);
            float motion=0.82+0.18*sin(p.x*2.1+p.z*1.7+uTime*0.6)*cos(p.y*1.4-uTime*0.4);
            return clamp((0.65*sides+0.35*corners)*depthEnvelope*motion,0.0,1.0);
        }
        // Bounded single scattering approximation with eight volume shadow steps.
        // No solid-geometry shadow maps or physically complete multiple scattering.
        bool oneSphereBlocksLight(vec3 point, vec3 towardLight, vec4 sphere) {
            if (sphere.w <= 0.0) return false;
            vec3 offset=point-sphere.xyz;
            float b=dot(offset,towardLight);
            float c=dot(offset,offset)-sphere.w*sphere.w;
            float discriminant=b*b-c;
            if (discriminant < 0.0) return false;
            float q=sqrt(discriminant);
            return (-b+q > 0.001) && (-b-q < 2.56);
        }
        bool sceneBlocksLight(vec3 point, vec3 towardLight) {
            for (int i=0; i<3; i++) {
                if (float(i) < uShadowCount && oneSphereBlocksLight(point,towardLight,uShadowSpheres[i])) return true;
            }
            return false;
        }
        float lightVisibility(vec3 point, vec3 towardLight) {
            if (sceneBlocksLight(point,towardLight)) return 0.0;
            float opticalDepth=0.0;
            for(int j=0;j<8;j++) {
                vec3 samplePoint=point+towardLight*(float(j)+0.5)*0.32;
                opticalDepth+=density(samplePoint)*0.32;
            }
            return exp(-opticalDepth*uLightExtinction);
        }
        void main() {
            vec2 ndc=vUv*2.0-1.0;
            vec3 dir=normalize(uForward + uRight*(ndc.x*uAspect*uTanHalfFov) + uUp*(ndc.y*uTanHalfFov));
            float nearT=0.15, farT=13.0;
            float sampledDepth=texture2D(uDepth,vUv).r;
            float ndcDepth=sampledDepth*2.0-1.0;
            float eyeDepth=(2.0*uNear*uFar)/(uFar+uNear-ndcDepth*(uFar-uNear));
            float cosForward=max(dot(dir,normalize(uForward)),0.0001);
            farT=min(farT,eyeDepth/cosForward);
            if(farT<=nearT) {gl_FragColor=vec4(0.0);return;}
            float stepLen=(farT-nearT)/float(STEPS);
            float transmittance=1.0;
            vec3 lightColor=vec3(0.8,0.87,1.0);
            vec3 accumulated=vec3(0.0);
            for(int i=0;i<STEPS;i++) {
                float t=nearT+(float(i)+0.5)*stepLen;
                vec3 samplePosition=uEye+dir*t;
            float d=density(samplePosition);
                float atten=exp(-d*stepLen*0.7);
                float scatter=(1.0-atten)*transmittance;
                float visibility=lightVisibility(samplePosition,normalize(uLightDirection));
            // Isotropic scattering: intentionally modest, no unsupported phase model claims.
            accumulated+=lightColor*scatter*visibility*0.20;
                transmittance*=atten;
            }
            gl_FragColor=vec4(accumulated,clamp(1.0-transmittance,0.0,0.42));
        }"""
    fun validate(stepsOverride:Int=steps) { require(stepsOverride in 1..128) }
}
