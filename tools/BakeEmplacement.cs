// Execute in Unity with tools/reference/GocEmplacementMotion.cs prepended.
// No scene objects or assets are changed. Output is a little-endian gzip stream.
internal class CommandScript : IRunCommand
{
    [System.Serializable] public class Document { public Block[] Blocks; }
    [System.Serializable] public class Block
    {
        public string Name; public int ObjectId, ParentId, BlockType;
        public UnityEngine.Vector3 Position, Rotation, Scale;
        public Properties Properties;
    }
    [System.Serializable] public class Properties
    {
        public int PrimitiveType; public UnityEngine.Color ColorRgba;
    }
    struct Key { public int Frame; public float[] Values; }
    static float[] Values(GocStrikeDemo.GocEmplacementMotion.Pose p, UnityEngine.Color c)
    {
        return new[] {p.Position.x,p.Position.y,p.Position.z,p.Rotation.x,p.Rotation.y,p.Rotation.z,p.Rotation.w,
            p.Scale.x,p.Scale.y,p.Scale.z,c.r,c.g,c.b,c.a};
    }
    static bool Same(float[] a,float[] b)
    {
        for(int i=0;i<a.Length;i++) if(System.Math.Abs(a[i]-b[i])>0.000001f) return false;
        return true;
    }
    public void Execute(ExecutionResult result)
    {
        string root=@"__PROJECT_ROOT__";
        var doc=new Document();
        using(var reader=new System.IO.BinaryReader(System.IO.File.OpenRead(@"__BAKE_INPUT__")))
        {
            doc.Blocks=new Block[reader.ReadInt32()];
            for(int i=0;i<doc.Blocks.Length;i++)
            {
                var b=new Block(); doc.Blocks[i]=b;
                b.Name=System.Text.Encoding.UTF8.GetString(reader.ReadBytes(reader.ReadInt32()));
                b.ObjectId=reader.ReadInt32();b.ParentId=reader.ReadInt32();b.BlockType=reader.ReadInt32();
                b.Position=new UnityEngine.Vector3(reader.ReadSingle(),reader.ReadSingle(),reader.ReadSingle());
                b.Rotation=new UnityEngine.Vector3(reader.ReadSingle(),reader.ReadSingle(),reader.ReadSingle());
                b.Scale=new UnityEngine.Vector3(reader.ReadSingle(),reader.ReadSingle(),reader.ReadSingle());
                b.Properties=new Properties{PrimitiveType=reader.ReadInt32(),ColorRgba=new UnityEngine.Color(reader.ReadSingle(),reader.ReadSingle(),reader.ReadSingle(),reader.ReadSingle())};
            }
        }
        string output=root+"/src/main/resources/assets/gocstrike/animation/emplacement.bin";
        using(var file=System.IO.File.Create(output))
        using(var writer=new System.IO.BinaryWriter(file))
        {
            writer.Write(0x474f4331); writer.Write(1);
            writer.Write(GocStrikeDemo.GocEmplacementMotion.Duration); writer.Write(20);
            writer.Write(3);
            foreach(int type in new[]{0,2,3})
            {
                var go=UnityEngine.GameObject.CreatePrimitive((UnityEngine.PrimitiveType)type);
                result.RegisterObjectCreation(go);
                var mesh=go.GetComponent<UnityEngine.MeshFilter>().sharedMesh;
                var vertices=mesh.vertices; var triangles=mesh.triangles;
                writer.Write(type); writer.Write(triangles.Length);
                foreach(int index in triangles)
                { var v=vertices[index]; writer.Write(v.x); writer.Write(v.y); writer.Write(v.z); }
                result.DestroyObject(go);
            }
            writer.Write(doc.Blocks.Length);
            var indices=new System.Collections.Generic.Dictionary<int,int>();
            for(int i=0;i<doc.Blocks.Length;i++) indices.Add(doc.Blocks[i].ObjectId,i);
            long total=0;
            foreach(var block in doc.Blocks)
            {
                writer.Write(block.ParentId<0?-1:indices[block.ParentId]);
                writer.Write(block.BlockType==1?block.Properties.PrimitiveType:-1);
                int group=-1,track,index,count,kind,appearance;
                bool moving=block.Name.StartsWith("working_") && int.TryParse(block.Name.Substring(8),out group);
                bool trace=GocStrikeDemo.GocEmplacementMotion.TryReadTrace(block.Name,out track,out index,out count);
                bool animatedColor=GocStrikeDemo.GocEmplacementMotion.TryReadAppearance(block.Name,out kind,out appearance);
                bool focus=block.Name.StartsWith("focus_");
                var rest=new GocStrikeDemo.GocEmplacementMotion.Pose(block.Position,block.Scale,UnityEngine.Quaternion.Euler(block.Rotation));
                if(block.ParentId<0)rest=new GocStrikeDemo.GocEmplacementMotion.Pose(UnityEngine.Vector3.zero,UnityEngine.Vector3.one,UnityEngine.Quaternion.identity);
                var color=block.BlockType==1?block.Properties.ColorRgba:UnityEngine.Color.white;
                int end=(moving||trace||animatedColor||focus)?UnityEngine.Mathf.CeilToInt(GocStrikeDemo.GocEmplacementMotion.Duration*20):0;
                var keys=new System.Collections.Generic.List<Key>();
                float[] last=null,previous=null;
                for(int frame=0;frame<=end;frame++)
                {
                    float t=UnityEngine.Mathf.Min(frame/20f,GocStrikeDemo.GocEmplacementMotion.Duration);
                    var pose=moving?GocStrikeDemo.GocEmplacementMotion.Sample(group,t):trace?
                        GocStrikeDemo.GocEmplacementMotion.SampleTrace(track,index,count,rest.Position,rest.Rotation,rest.Scale,t):rest;
                    var c=color;
                    if(animatedColor)
                    {
                        float gain=GocStrikeDemo.GocEmplacementMotion.AppearanceGain(kind,appearance,t);
                        if(kind==3)c=UnityEngine.Color.Lerp(c,new UnityEngine.Color(c.r*1.4f,c.g*.28f,c.b*1.7f,1f),gain);
                        else {c.r*=gain;c.g*=gain;c.b*=gain; if(kind==2 && gain<=0)c.a=0;}
                    }
                    if(focus)c=GocStrikeDemo.GocEmplacementMotion.FinalFlareColour(UnityEngine.Mathf.Floor(t*8)/8,
                        GocStrikeDemo.GocEmplacementMotion.ConvergenceRadiance(t));
                    float max=UnityEngine.Mathf.Max(1f,UnityEngine.Mathf.Max(c.r,UnityEngine.Mathf.Max(c.g,c.b)));
                    c.r=UnityEngine.Mathf.Sqrt(c.r/max);c.g=UnityEngine.Mathf.Sqrt(c.g/max);c.b=UnityEngine.Mathf.Sqrt(c.b/max);
                    var values=Values(pose,c);
                    foreach(float v in values)if(float.IsNaN(v)||float.IsInfinity(v))throw new System.Exception("Non-finite pose: "+block.Name+" at "+t);
                    if(last==null || !Same(last,values))
                    {
                        if(keys.Count>0 && keys[keys.Count-1].Frame<frame-1)keys.Add(new Key{Frame=frame-1,Values=previous});
                        keys.Add(new Key{Frame=frame,Values=values}); last=values;
                    }
                    previous=values;
                }
                writer.Write(keys.Count); total+=keys.Count;
                foreach(var key in keys){writer.Write(key.Frame);foreach(float v in key.Values)writer.Write(v);}
            }
            result.Log("Baked {0} nodes, {1} sparse keys to {2}",doc.Blocks.Length,total,output);
        }
        result.Log("Uncompressed bytes: {0}",new System.IO.FileInfo(output).Length);
    }
}
