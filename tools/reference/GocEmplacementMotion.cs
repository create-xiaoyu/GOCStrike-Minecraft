using UnityEngine;

namespace GocStrikeDemo
{
    /// <summary>"The Emplacement": absolute-time, reversible choreography for the 118.698667s score.
    /// Pure function of (group, time); the same shape as GocStrikeMotion, lighting is per-light.</summary>
    public static class GocEmplacementMotion
    {
        public const float SourceStart = 0f;            // the cut starts at source zero: score time == source time
        public const float Duration = 118.698667f;      // bar 45 downbeat
        public const float Impact = 73.365333f;         // global positive-spectral-flux maximum
        public const float SealComplete = 72.512f;  // the crystal path is fully inscribed
        public const float FlashAt = Impact;          // convergence completes on the preceding accent
        public const float Arrival = 72.981333f;        // the round reaches the target plane
        public const float RecedeAt = 113.322667f;      // subtraction begins
        public const float WhiteoutAt = 116.032f;       // final hit of the climax
        public const float ClearAt = WhiteoutAt + .45f;
        public const float WhiteFlashSeconds = 4.666667f;
        public const float KillAt = WhiteoutAt + .75f;  // let the final flash register before the death screen

        public const float Beat = 2f / 3f;              // 90.000 BPM
        public const float Bar = 4f * Beat;
        public const float Phrase = 4f * Bar;
        public const float BarPhase = 1.365333f;        // bar lines at BarPhase + n*Bar

        public const float WorldScale = 2.4f;           // world_y = 300 + WorldScale * local_y
        // Two kinds of height, and only one survives a WorldScale change. A plane the machine STANDS
        // ON is world metres written over WorldScale, so it holds its world altitude at any scale; a
        // height OF the machine is local and shrinks with it. Every altitude here is therefore the
        // apron plus a local proportion, and the working scales about the apron rather than about
        // world 300 - drop WorldScale with a raw local DeckY and the apron sinks into Gate A.
        public const float LowerDeckLift = 22f;         // world m over 300: the lower deck plane, world 322
        public const float ApronRise = 1.26f;           // world m of apron above it, so DeckY is world 323.26
        public const float DeckY = LowerDeckLift/WorldScale+ApronRise/WorldScale;   // summed AFTER the
                                                        // divide: that is the 6.461111f literal bit for bit
        public const float SealRise = 17.538889f;       // the working's rest altitude above the apron, and
        public const float SealHeight = DeckY+SealRise; // the 4.11 conduit run; SealHeight is 24 local at 3.6
        public const float ArrivalLift = 4.5f;          // the projection forms high and descends onto SealHeight
        // Where the stack contacts on the final hit. "Too close" measured as TOO LOW: the plate's
        // width is set by its own rest radius, but its altitude is this one number, and the
        // WorldScale 3.6 -> 2.4 rescale shrank the CLEARANCE with everything else (3.038889 local
        // was 10.94 m of air over the apron at 3.6 and only 7.29 m at 2.4) while the plate stayed
        // 91 m across. 6.25 puts the seat plane 15.00 m over the apron and 38.26 m over the Gate A
        // ground; the stack's LOWEST renderer goes 30.362 -> 38.049 m over the ground, 7.10 -> 14.79
        // over the apron and 5.30 -> 12.99 over an apron-standing eye. Journey 3 slows -1.63 ->
        // -1.27 m/s mean. From a player at the Surface Gate door, 36.8 m out, the stack's peak
        // vertical span falls 110.3 -> 81.1 deg of the 70 deg game frame.
        // The muzzle collar tracks SealSeatY-LayerGap, so B24's 25 mm contact is unaffected.
        public const float SealSeatY = DeckY+6.25f;
        public const float LayerGap = .12f;
        public const int MovingGroups = 50;
        // 16-20 are inner crystals, 37-41 outer crystals; 22-24/30-31 carry the folded prism.
        // 5 is the central star; 21 strikes the ground; 25 is the impact ball; 26-29 are strike rings; 42-48 carry seed and light drops.
        public const float Hidden = .0001f;
        public const float CrystalLift = 5.5f;          // local: the crystals' heel over the deck plane
        // M-A: how far under the working's own plane the Shaper stops. A constant because the
        // aperture law reads the same number - the mantle's top is
        // stackY + LayerOffset[5]*gap - Throat, so the step rides journey 3 down 46.5 m and
        // delaminates with the stack instead of being pinned to a rest altitude.
        public const float Throat = .80f;
        public const float DrumTop = DeckY+.015f*260f;      // the drum's top face; 10.361111 at 3.6
        public const float MuzzleY = DrumTop+2f*25f*.12f;   // the barrel's muzzle; 16.361111 at 3.6

        public const float Emplace = 1.344f;            // first scribe completes
        public const float Mains = 12.010667f;          // power on
        public const float MuzzleAt = 20.010667f;       // the muzzle is laid
        public const float Shimmer = 22.677333f;        // breech build
        public const float BreechLoad = 30.677333f;     // round loaded
        public const float Seat = 41.344f;              // drum seated
        public const float Charge = 44.010667f;         // the barrel extends
        public const float Aim = 52.010667f;            // alignment; the marks ride up
        public const float Pressure = 62.677333f;       // overpressure; the marks lock
        public const float Cut = 65.344f;               // the music collapses
        public const float Floor = 69f;                 // measured RMS floor; absolute stillness
        public const float WindUp = 69.632f;            // the round sinks
        public const float Launch = 70.677333f;         // the shot
        public const float Strike = 84.032f;            // second major impact: the column hits the ground
        public const float Escalate1 = 84.032f;         // MULTIPLICITY
        public const float Escalate2 = 94.698667f;      // INVERSION
        public const float Escalate3 = 105.365333f;     // SATURATION
        public const float ExitFrom = 110.677333f;      // the outside-in dissolve begins

        // Boundary convention: build cues (t < Impact) are the detected accents, a constant -21.3 ms
        // (one cue frame) early because soft orchestral attacks read early; climax cues are the exact
        // grid, where detection error is 0.0 ms. Never mix the two.
        public static readonly float[] Accents = {
            1.344f,1.514667f,1.856f,2.688f,4.010667f,4.181333f,5.354667f,5.845333f,
            6.677333f,6.848f,7.189333f,8.021333f,8.341333f,9.344f,10.688f,11.178667f,
            12.010667f,13.354667f,14.677333f,16.021333f,17.344f,18.688f,19.008f,20.010667f,
            21.354667f,22.677333f,24.021333f,24.341333f,25.344f,26.666667f,27.008f,28.010667f,
            29.354667f,29.674667f,30.677333f,33.344f,34.688f,35.008f,36.010667f,37.674667f,
            38.336f,38.677333f,40.021333f,40.341333f,41.344f,44.010667f,44.330667f,45.354667f,
            45.674667f,46.336f,46.677333f,47.018667f,48.021333f,48.341333f,49.002667f,49.344f,
            49.685333f,50.688f,51.008f,52.010667f,52.352f,53.333333f,53.674667f,54.016f,
            54.357333f,54.677333f,54.997333f,56.021333f,56.341333f,57.002667f,57.344f,57.514667f,
            57.685333f,58.688f,59.008f,59.690667f,60.010667f,60.352f,60.693333f,61.354667f,
            61.674667f,62.677333f,63.018667f,64.341333f,64.512f,65.002667f,65.344f,70.677333f,
            71.146667f,71.637333f,72f,72.512f,72.981333f,73.365333f,73.557333f,74.346667f,
            74.538667f,74.666667f,74.88f,75.2f,75.712f,76.032f,76.501333f,76.992f,
            77.333333f,77.546667f,77.824f,78.336f,78.677333f,79.68f,80.042667f,80.512f,
            81.322667f,81.856f,82.325333f,82.709333f,83.178667f,83.669333f,84.032f,85.013333f,
            85.354667f,85.866667f,86.336f,86.698667f,87.168f,87.658667f,88.021333f,89.002667f,
            89.344f,90.218667f,90.346667f,90.688f,91.178667f,91.989333f,92.522667f,92.992f,
            93.333333f,93.845333f,94.336f,94.698667f,95.68f,96.021333f,96.554667f,97.045333f,
            97.365333f,97.834667f,98.325333f,98.88f,99.669333f,100.010667f,101.013333f,101.354667f,
            101.845333f,102.656f,103.168f,103.658667f,104f,104.512f,105.024f,105.365333f,
            106.346667f,106.688f,107.2f,108.032f,108.501333f,109.013333f,109.354667f,110.336f,
            110.677333f,111.232f,111.68f,112.021333f,112.512f,113.322667f,114.709333f,115.178667f,
            115.669333f,116.032f,117.354667f
        };
        public static readonly float[] IdleCues = {1.344f,1.514667f,1.856f,2.688f,4.010667f,4.181333f,
            5.354667f,5.845333f,6.677333f,6.848f,7.189333f,8.021333f,8.341333f,9.344f,10.688f,11.178667f};
        public static readonly float[] ThudCues = {12.010667f,13.354667f,14.677333f,17.344f,18.688f,
            22.677333f,24.021333f,25.344f,28.010667f,29.354667f,33.344f,34.688f,36.010667f,38.677333f,
            40.021333f,41.344f};
        public static readonly float[] ChargeCues = {44.010667f,44.330667f,45.354667f,45.674667f,46.336f,
            46.677333f,47.018667f,48.021333f,48.341333f,49.002667f,49.344f,49.685333f,50.688f,51.008f,
            52.010667f,52.352f,53.333333f,53.674667f,54.016f,54.357333f,54.677333f,54.997333f,56.021333f,
            56.341333f,57.002667f,57.344f,57.514667f,57.685333f,58.688f,59.008f,59.690667f,60.010667f,
            60.352f,60.693333f,61.354667f,61.674667f,62.677333f,63.018667f,64.341333f,64.512f,65.002667f,
            65.344f};
        public static readonly float[] CollarDrop = {2.688f,4.010667f,5.354667f,6.677333f,8.021333f};
        public static readonly float[] CollarLock = {4.181333f,5.845333f,7.189333f,8.341333f,9.344f};
        public static readonly float[] RiserCues = {70.677333f,71.146667f,71.637333f,72f,72.512f,72.981333f};
        public static readonly float[] FrontCues = {73.365333f,73.557333f,74.346667f,74.538667f};
        public static readonly float[] MarkCues = {74.666667f,74.88f,75.2f,75.712f,76.032f};
        public static readonly float[] ReboundCues = {76.501333f,77.333333f,78.336f};
        public static readonly float[] LanceCues = {84.032f,85.013333f,85.866667f,86.698667f,87.658667f};
        public static readonly float[] RepriseCues = {105.365333f,106.346667f,106.688f,107.2f,108.032f};
        public static readonly float[] BloomCues = {Impact,84.032f,94.698667f,105.365333f};
        public static readonly float[] SecondaryBlooms = {81.322667f,91.989333f,102.656f};
        public static readonly float[] ExitCues = {110.677333f,111.68f,112.512f,113.322667f,
            114.709333f,115.178667f,115.669333f};

        /// F3's six telescope stages, as INDICES into ChargeCues: 44.011, 45.355, 46.677, 48.021,
        /// 49.344, 52.011. Indices rather than a sixth float array so there is no second copy of a
        /// measured time to drift, and so cues/in-accents has nothing new to police.
        static readonly int[] Telescope = {0,2,5,7,10,14};
        static readonly float[] FanOut = {75.2f,74.538667f,74.346667f,74.88f,73.557333f,73.365333f};
        static readonly float[] FanDwell = {1.6f,1.1f,1f,1.4f,.9f,.55f};
        static readonly int[] LayerOffset = {0,1,2,-1,3,4};
        // Five three-edge paths partition study A: outer edge, connecting ray, inner edge.
        // Vertices 0-4 are outer crystals, 5-9 are inner crystals.
        public const int ReflectionLegs=3;
        static readonly int[] ReflectionRoute={0,1,6,7};
        public static int ReflectionVertex(int ray,int point)
        {
            int vertex=ReflectionRoute[point];
            return (vertex<5?0:5)+(vertex%5+ray)%5;
        }
        public static float ReflectionDeparture(int index)
        {
            int ray=index/ReflectionLegs,phase=index%ReflectionLegs;
            int destination=ReflectionVertex(ray,ReflectionLegs)-5;
            return RiserCues[destination]-(ReflectionLegs-phase)*.8f;
        }
        static readonly Vector3[] ReflectionTints = {
            new Vector3(.6f,3.5f,5.5f), new Vector3(5.5f,3.3f,.45f),
            new Vector3(2.6f,1f,5.5f), new Vector3(5.5f,.75f,1.7f), new Vector3(.7f,5.5f,2.8f)
        };
        public static Vector3 ReflectionTint(int ray) => ReflectionTints[ray];

        public struct Pose
        {
            public Vector3 Position, Scale;
            public Quaternion Rotation;
            public Pose(Vector3 p, Vector3 s, Quaternion r) { Position=p; Scale=s; Rotation=r; }
        }

        static float N(float a, float b, float t) => Mathf.Clamp01((t-a)/(b-a));
        /// Clamped to make 0..1 total: in float32 the quintic rounds ABOVE 1 near x=1 (worst +1.07e-6
        /// over every float in [0,1]) and Open() feeds 1-Smoother to Mathf.Pow, where a base one ulp
        /// under zero is NaN, not a rounding error. The cubic never does, but carries the same contract.
        public static float Smooth(float a, float b, float t) { float x=N(a,b,t); return Mathf.Clamp01(x*x*(3f-2f*x)); }
        public static float Smoother(float a, float b, float t) { float x=N(a,b,t); return Mathf.Clamp01(x*x*x*(x*(6f*x-15f)+10f)); }
        public static float EaseOut(float a, float b, float t, float p=3f) => 1f-Mathf.Pow(1f-N(a,b,t),p);
        public static float EaseIn(float a, float b, float t, float p=2.4f) => Mathf.Pow(N(a,b,t),p);
        public static float Back(float a, float b, float t, float k=1.7f) { float y=1f-N(a,b,t); return 1f-y*y*((k+1f)*y-k); }
        public static float Elastic(float a, float b, float t, float c=2.4f, float d=5.5f)
        { float x=N(a,b,t); return 1f-Mathf.Exp(-d*x)*Mathf.Cos(c*Mathf.PI*x); }
        public static float Impulse(float cue, float t, float attack=.06f, float decay=4f)
        { float a=t-cue; if(a<0f)return 0f; return a<attack?Smooth(0f,attack,a):Mathf.Exp(-(a-attack)*decay); }
        public static float Stagger(int i, int n, float start, float span, float dwell, float t)
        { float o=start+span*(n<=1?0f:i/(float)(n-1)); return EaseOut(o,o+dwell,t); }
        /// EaseOut's arrival shape over a C2 foot: the only curve allowed to carry a scale out of Hidden.
        public static float Open(float a, float b, float t, float p=2.6f) => 1f-Mathf.Pow(1f-Smoother(a,b,t),p);
        /// A reveal that grows GEOMETRICALLY: log(size) follows Smoother from `floor` up to 1, so every
        /// tick is a similar ratio and none stands out from its neighbours. A polynomial foot sampled
        /// just past its start leaps hundreds of times in one tick; this swells from a point instead,
        /// with most of the visible growth in the window's last third. 0 before a.
        public static float Swell(float a, float b, float t, float floor=.001f)
        { float u=Smoother(a,b,t); return u<=0f?0f:Mathf.Pow(floor,1f-u); }

        /// Uniform reveal. Continuous from zero: the sentinel is only reached at a genuinely invisible size.
        public static Vector3 Grow(Vector3 rest, float v)
        { Vector3 s=rest*v; return (s.x<=Hidden||s.y<=Hidden||s.z<=Hidden)?Vector3.one*Hidden:s; }
        /// Radial iris. XZ opens together from zero, Y is untouched; shear-free on Y-rotated ring cubes.
        public static Vector3 Iris(Vector3 rest, float v)
        { Vector3 s=new Vector3(rest.x*v,rest.y,rest.z*v); return (s.x<=Hidden||s.z<=Hidden)?Vector3.one*Hidden:s; }
        /// Base pin for an (r,H,r) ring: puts the bottom of the H-tall plate wall exactly on deck.
        public static float LouvreBase(float deck, float H, float barWidth=.015f) => deck+.5f*barWidth*H;

        /// F4's overpressure vent: the shroud lifts off the drum top on the strongest downbeat of
        /// B11 and holds, so its louvres stand clear of the wall they capped and the gap reads as
        /// venting. It folds back into the same retraction that flattens the machine for B13, so
        /// the silence is still absolutely still.
        static float Vent(float t) => .625f*Smoother(Pressure,Pressure+.55f,t)*(1f-EaseIn(Cut+.056f,67.6f,t,2.2f));
        /// The shroud wall's own height, shared so F1's muzzle collar can ride its rim. Written
        /// once here and read by both g21 and g26: they disagreed silently for the life of the
        /// piece, which is how 56 primitives ended up 5.0 m down a 12.6 m well.
        static float ShroudH(float t)
        {
            // F2 moved this from ThudCues[10..15] to [13..15]: the cap goes on AFTER the breech
            // wall has closed over the round, and it grows from zero on the same cue its iris
            // opens on, so the reveal has a foot instead of appearing at 19.8 % of its peak.
            float H=150f*Ratchet(ThudCues,13,3,t,1.4f);
            return Mathf.Lerp(H,2f,EaseIn(Cut+.056f,67.6f,t,2.2f));
        }
        /// F1: where the muzzle collar sits. The drum top is the floor of the shroud's well, not a
        /// rim - the collar reads on the drum at t=30 and is GONE at t=45 and t=62. Max() so the
        /// collar never drops below its own laid station when the shroud retracts for B13. The
        /// standoff is .70 and not the .16 I first rendered: the collar's outer key tip is 3.64 m
        /// against the shroud's 6.24 m mouth, so flush on the rim read as a decal drawn on the cap
        /// rather than a part. 1.68 m of air puts a lit ring INSIDE the mouth, which is what an
        /// aperture over a vent looks like, and it makes the bore SAFER rather than tighter: the
        /// shroud has retracted to H 2 by 67.6, so at Launch this is DrumTop+.73, the round meets
        /// the hoop 0.1 s LATER at t=71.300 instead of 71.200, and by then it has narrowed 2.6 %.
        /// Measured, not assumed: clearance 0.5364 m -> 0.5943 m.
        static float MuzzleStation(float t) => Mathf.Max(DrumTop+.12f,DrumTop+.015f*ShroudH(t)+Vent(t)+.70f);
        /// H1: one shared ramped azimuth for the five deck stations, so they stop sitting at five
        /// frozen screen positions for 40 s. It starts once the pylons have finished expanding and
        /// ends exactly one station on. NOT applied to 37-41: precessing the crystals drives the
        /// clearance law's satellite<->crystal minimum from +4.679 m to -7.488 m at 36 deg.
        static float Precess(float t) => 72f*Smoother(Impact+4f,ExitFrom,t);

        public static float Pulse(float t, float attack=.035f, float decay=12f)
        {
            int lo=0,hi=Accents.Length-1;
            while(lo<=hi){int m=(lo+hi)/2;if(Accents[m]<=t)lo=m+1;else hi=m-1;}
            if(hi<0)return 0f;
            float a=t-Accents[hi];
            if(a>=attack)return Mathf.Exp(-(a-attack)*decay);
            // The attack ramps from where the previous accent's tail already is, so an onset can only
            // rise. The closest accent pair is 0.128 s, well past attack, so that tail is its decay.
            return Mathf.Lerp(hi>0?Mathf.Exp(-(t-Accents[hi-1]-attack)*decay):0f,1f,Smooth(0f,attack,a));
        }
        /// Shapes entry to and exit from the 5.33s accent hole; the re-entry ramp finishes before the riser.
        public static float AccentGain(float t) => Mathf.Clamp01(1f-Smoother(64.6f,Cut,t)+Smoother(WindUp,70.6f,t));

        /// Integral of Smoother from a to t, so a decelerating rate can be subtracted in closed form.
        public static float SmootherArea(float a, float b, float t)
        {
            float w=b-a, x=Mathf.Clamp01((t-a)/w);
            float area=w*(x*x*x*x*(x*x-3f*x+2.5f));
            return t>b?area+(t-b):area;
        }
        /// Angle whose RATE ramps 0 -> omega with time constant tau from t0, then back to exactly zero
        /// across [stopFrom, stopTo]. C1: the angle itself never jumps.
        public static float Spin(float t, float t0, float omega, float tau, float stopFrom, float stopTo)
        {
            float a=Mathf.Max(0f,t-t0);
            float up=a-tau*(1f-Mathf.Exp(-a/tau));
            return omega*(up-SmootherArea(stopFrom,stopTo,t));
        }
        /// One smoothed step per measured cue, averaged to 0..1 - the piece's one rhythmic motif.
        public static float Ratchet(float[] cues, int from, int count, float t, float dwell=.45f)
        { float a=0f; for(int i=0;i<count;i++)a+=Smoother(cues[from+i],cues[from+i]+dwell,t); return a/count; }
        /// The same motif on a uniform grid, where there is no cue array to walk: a mechanical index
        /// that accelerates into every step and arrests. Continuous - a step ends at exactly n+1.
        /// stopTo>stopFrom settles the index onto its nearest whole step across that window, so a
        /// mechanism arrests on the final hit (B23) instead of indexing off the end of the piece.
        public static float RatchetGrid(float t0, float period, float t, float dwell=.45f, float stopFrom=0f, float stopTo=0f)
        {
            float u=Mathf.Max(0f,(t-t0)/period), n=Mathf.Floor(u);
            float live=n+Smoother(0f,dwell/period,u-n);
            if(stopTo<=stopFrom)return live;
            return Mathf.Lerp(live,Mathf.Round(Mathf.Max(0f,(stopFrom-t0)/period)),Smoother(stopFrom,stopTo,t));
        }
        /// Weighted ratchet: later steps are larger, so a run reads as an acceleration.
        public static float RatchetAccel(float[] cues, float t, float dwell, float p=2f)
        { float a=0f,w=0f; for(int i=0;i<cues.Length;i++){float k=Mathf.Pow(i+1,p);w+=k;a+=k*Smoother(cues[i],cues[i]+dwell,t);} return a/w; }

        /// Per-class exit on a measured accent: shrink AND recede, so the object loses pixels two ways.
        public static float ExitScale(int group, float t) { float cue=ExitCue(group); return 1f-EaseIn(cue,cue+2.8f,t,2.2f); }
        /// The terminal dissolve, staggered per group; reaches Hidden before ClearAt by construction.
        public static float Dissolve(int group, float t) { float a=WhiteoutAt+(group%7)*.02f; return 1f-EaseIn(a,a+.30f,t,2.2f); }
        static float ExitCue(int g)
            => g>=27&&g<=31 ? ExitCues[0]
             : g>=6&&g<=10  ? ExitCues[1]
             : g>=11&&g<=15 ? ExitCues[2]
             : g>=37        ? ExitCues[3]
             : g>=32&&g<=36 ? ExitCues[4]
             : g>=18&&g<=21 ? ExitCues[5]
             : g>=22&&g<=24 ? ExitCues[6]
             : 1e9f;

        /// The disk settles at altitude, then descends gently with upper ring layers leading.
        public static float StackY(float t,int layerDelay=0)
        {
            float arrive=Mathf.Lerp(SealHeight+ArrivalLift,SealHeight,Smoother(Impact,Impact+3.2f,t));
            return arrive-.25f*(SealHeight-SealSeatY)*Smoother(Escalate2+layerDelay*.4f,WhiteoutAt,t);
        }
        /// Inter-layer spacing. Delamination is confined to the INVERSION phrase 94.698667 -> 105.365333,
        /// the one window where the 0.44 m adjacent-gap rule is deliberately exceeded.
        public static float Gap(float t)
        {
            // F7/H3: the target is 1.4 local (3.36 m adjacent, 26.9 m of stack span) and the ramp
            // opens over 3 s instead of 4 and arrests over 103.658667 -> Escalate3 instead of
            // 103.168, so the cone is open across its whole phrase and shuts on the downbeat. Both
            // ends stay strictly inside the law's exemption window, which IS Escalate2..Escalate3 -
            // opening one frame earlier than Escalate2 would put a 3.36 m gap outside it and break
            // registration. The close is 1.707 s for 12.3 m of travel on g05, 5.6 local/s.
            float g=LayerGap*Mathf.Lerp(1f,.18f,Smoother(RecedeAt,WhiteoutAt,t));
            float split=Smoother(Escalate2,Escalate2+3f,t)-Smoother(103.658667f,Escalate3,t);
            return Mathf.Lerp(g,1.4f,Mathf.Clamp01(split));
        }
        /// Radial ripple: nine concentric rings instead of one plate, for one float and zero primitives.
        static float Ripple(int layer, float t)
            => 1f+.012f*Smoother(76.032f,78.698667f,t)*Mathf.Sin(2f*Mathf.PI*(t-layer*.18f)/Bar);
        /// INVERSION: adjacent stack layers counter-rotate, ramped on the rate so no angle steps, and
        /// ramped back to exactly zero across RecedeAt->WhiteoutAt like every Spin (B23).
        static float CounterRotate(int group, float t)
            => -2f*(3f+group)*(SmootherArea(Escalate2,Escalate2+1.5f,t)-SmootherArea(RecedeAt,WhiteoutAt,t));

        static float NearestCue(float[] cues, float t)
        { float best=-1e9f; for(int i=0;i<cues.Length;i++){if(cues[i]<=t)best=cues[i];else break;} return best; }
        static float NearestThud(float t) => NearestCue(ThudCues,t);
        static float NearestChargeCue(int station, float t)
        { float best=-1e9f; for(int k=0;k<ChargeCues.Length;k++){if(ChargeCues[k]>t)break;if(k%5==station)best=ChargeCues[k];} return best; }

        /// The round's own altitude: fed into the drum, wound up, then the shot on five visible shoves.
        /// The sixth riser cue IS Arrival, so instead of a shove it fades the other five out across the
        /// clench: the round settles onto its arrival altitude and is exactly there at Impact.
        public static float RoundY(float t)
        {
            float y=Mathf.Lerp(DeckY-.9f,DeckY+1.688889f,Smoother(BreechLoad,BreechLoad+1.6f,t));
            y=Mathf.Lerp(y,DeckY+1.338889f,EaseIn(WindUp,Launch,t,2f));
            y=Mathf.Lerp(y,SealHeight+ArrivalLift,EaseIn(Launch,Arrival,t,1.7f));
            float settle=1f-Smoother(Arrival,Impact,t);
            for(int i=0;i<RiserCues.Length-1;i++)y+=.9f*Impulse(RiserCues[i],t,.06f,6f)*settle;
            return y;
        }
        /// Shared hand-off for groups 18-21: the ring is thrown outward as a pressure front, inhales
        /// back into frame at the INVERSION downbeat, and re-fires.
        static void ToFront(int i, float t, float kMach, float HMach, ref float k, ref float H, ref float y)
        {
            float age=t-FrontCues[i];
            if(age<0f)return;
            float land=Smoother(0f,.8f,age);
            k=Mathf.Lerp(kMach,42f+i*4f,Smoother(0f,5.333333f,age));
            H=Mathf.Lerp(HMach,1f,land);
            y=Mathf.Lerp(y,DeckY+.1f+i*.35f,land);
            // B20's inhale, held OUTSIDE the seal. At .42 the four fronts pulled back to r 42-58 m -
            // inside the plate's own 45 m rim, 42 m under it - so a standing eye saw a plain
            // unornamented hoop sweep across the payload. .70 holds them at 71-93 m, where the near
            // arc stays below the disk's lower rim: measured screen occlusion of the seal by g18-21
            // through the whole inhale is 0.00 % at 20, 36.8, 45 and 60 m of viewing distance.
            // A ring is always in front of the disk for a viewer standing on its own radius, and no
            // factor fixes that: 6.1 % at 95 m and 12.6 % at 120 m are that case, not a defect.
            float inhale=Mathf.Clamp01(Smoother(Escalate2-6f,Escalate2,t)-Smoother(Escalate2,Escalate2+1.8f,t));
            k*=Mathf.Lerp(1f,.70f,inhale);
            // H2: the fronts LIFT on the inhale, with a per-index amplitude. Four identical arcs is
            // the complaint, not four arcs - so the outer ring climbs furthest and the four stop
            // being one thick band at the bottom of frame. Measured at the t=94.699 framing (eye
            // 116.3 m, 142.8 m out) the four visible far arcs spread 2.5 deg -> 5.0 deg and the
            // outermost still sits 3.0 deg BELOW the seal's own far rim, so the 0.00 % occlusion
            // the paragraph above holds for is unchanged. 5.65 local over the 6 s ramp is
            // 1.8 local/s, and the 1.8 s arrest is 5.9 local/s, both far under the 12 cap.
            y=Mathf.Lerp(y,DeckY+1.1f+i*1.9f,inhale);
        }

        public static Pose Sample(int group, float time)
        {
            float t=Mathf.Clamp(time,0f,Duration);
            float dissolve=Dissolve(group,t), exit=ExitScale(group,t);
            float stackY=StackY(t), gap=Gap(t);
            float sealRad=1f+.12f*Smoother(Impact,Impact+2f,t);
            Vector3 p=Vector3.zero, s=Vector3.one;
            Quaternion r=Quaternion.identity;
            if(group<6) // The projection: it does not exist until the round it fires reaches altitude.
            {
                float cue=FanOut[group];
                float radius=sealRad*Open(cue,cue+FanDwell[group],t,2.6f)*dissolve;
                float ang=(group%2==0?1f:-1f)*Spin(t,cue,3f+group,1.5f,RecedeAt,WhiteoutAt);
                p=new Vector3(0f,StackY(t,Mathf.Max(0,3-LayerOffset[group]))+LayerOffset[group]*gap,0f);
                s=Iris(Vector3.one,radius);
                r=Quaternion.Euler(0f,ang,0f);
            }
            else if(group<11) // Anchor collars sited on the deck, then the authored backlash sinks.
            {
                int i=group-6; float drop=CollarDrop[i], lockAt=CollarLock[i];
                float a=(i*72f+Precess(t))*Mathf.Deg2Rad;                // H1
                float land=EaseOut(drop,drop+.9f,t,3f), leave=EaseIn(ExitCues[1],ExitCues[1]+2.8f,t,2.2f);
                // Settle above the deck, then leave smoothly.
                float y=Mathf.Lerp(DeckY+3.1f,DeckY,land)
                       -.15f*Smoother(Launch,Arrival,t)+9f*leave;
                float radius=17f*(1f+.06f*leave);
                float brim=1f+.5f*Smoother(LanceCues[i],LanceCues[i]+1.2f,t)*(1f-Smoother(Escalate2,97f,t));
                // F6: the sinks overflow at SATURATION. The carrier plunges onto this sink's throat
                // on the same cue and lands 0.55 s in, so the brim is still rising as it arrives.
                brim+=.42f*Smoother(RepriseCues[i],RepriseCues[i]+1.2f,t)*(1f-Smoother(ExitCues[0],ExitCues[0]+1.4f,t));
                float sweep=9.365333f+(i<2?0f:.490667f);
                p=new Vector3(Mathf.Sin(a)*radius,y,Mathf.Cos(a)*radius);
                s=Grow(Vector3.one,Smoother(drop-.35f,drop+.9f,t)*Smoother(SeedOpen+.2f+i*.35f,SeedOpen+1f+i*.35f,t)*brim*dissolve*exit);
                r=Quaternion.Euler(0f,-i*72f-Precess(t)+90f*Smoother(lockAt,lockAt+.6f,t)+72f*Smoother(sweep,sweep+1.1f,t)
                                     -Spin(t,Impact,10f,1.5f,RecedeAt,WhiteoutAt),0f);
            }
            else if(group<16) // Ranging marks: deck station, ride to the target plane, then satellites.
            {
                int i=group-11; float wake=Shimmer+i*.4f, ride=Aim+i*1.333333f;
                float radius=Mathf.Lerp(11.5f,21.8f,Smoother(ride,ride+4f,t));
                radius=Mathf.Lerp(radius,26.6f,Smoother(Escalate1,Escalate1+4f,t));
                float y=Mathf.Lerp(DeckY+1.2f,DeckY+5.8f,Smoother(wake,Charge,t));
                y=Mathf.Lerp(y,SealHeight+LayerGap,Smoother(ride,ride+4f,t));
                y=Mathf.Lerp(y,stackY+gap,Smoother(Escalate1,Escalate1+.5f,t));
                // INVERSION drops them to the lowest tier: 6 local under the plate and still at
                // r 26.6, outside the crystals' ring and the plate's rim. Seen from the ground,
                // lower-and-further-out projects OUTSIDE what is above it, so no satellite is ever
                // drawn over the plate's inscription bands.
                y=Mathf.Lerp(y,stackY-6f,Smoother(Escalate2-1.5f,Escalate2+2f,t));
                y+=8f*EaseIn(ExitCues[2],ExitCues[2]+2.6f,t,2.2f);
                float ao=(i*72f+36f+Spin(t,Escalate1,7f,2.2f,RecedeAt,WhiteoutAt))*Mathf.Deg2Rad;
                p=new Vector3(Mathf.Sin(ao)*radius,y,Mathf.Cos(ao)*radius);
                s=Grow(Vector3.one,Smoother(wake,wake+1.8f,t)*dissolve*exit);
                r=Quaternion.Euler(0f,-Spin(t,Impact,14f,1.5f,RecedeAt,WhiteoutAt)+i*72f,0f);
            }
            else if(group==42)
            {
                p=new Vector3(0f,DeckY+CrystalLift+2.8f,0f);
                float life=Smoother(SeedRise+.7f,SeedRise+1f,t)*(1f-Smoother(SeedOpen-1.2f,SeedOpen-.3f,t));
                s=Grow(Vector3.one,1.4f*life*FlickerScale(10,t));
            }
            else if(group==49)
            {
                float age=t-SeedRise;
                if(age<0f || age>2.2f)return new Pose(Vector3.zero,Vector3.one*Hidden,Quaternion.identity);
                float height=DeckY+CrystalLift+2.8f;
                float launch=Mathf.Clamp01(age/.8f);
                float fly=Mathf.Clamp01((age-.8f)/1.4f);
                p=new Vector3(0f,Mathf.Lerp(1f,height,launch)+32f*fly,0f);
                s=Grow(new Vector3(.45f,2.4f,.45f),Smoother(0f,.08f,age)*(1f-Smoother(1.5f,2.2f,age)));
                r=Quaternion.identity;
            }
            else if(group==43)
            {
                p=new Vector3(0f,DeckY+CrystalLift+2.8f,0f);
                float reveal=Smoother(SeedOpen,SeedOpen+.12f,t);
                s=t>=SeedClear?Vector3.one*Hidden:Iris(Vector3.one,Mathf.Lerp(.7f,25f,Smoother(SeedOpen,SeedClear,t))*reveal);
            }
            else if(group>=44 && group<=48)
            {
                int satellite=group-44;
                float begin=Strike+2f+satellite*.65f, age=t-begin;
                if(age<0f || age>=2.5f)return new Pose(Vector3.zero,Vector3.one*Hidden,Quaternion.identity);
                var source=Sample(11+satellite,t).Position;
                p=source-Vector3.up*(age*8f);
                s=Grow(Vector3.one,.45f*Smoother(0f,.15f,age)*(1f-Smoother(1.7f,2.5f,age)));
            }
            else if(group==21)
            {
                float width=(1.4f+.8f*ConvergenceBoost(t))*(1f-Smoother(RecedeAt,ClearAt,t));
                return BeamPose(Sample(5,t).Position,Vector3.zero,
                    Smoother(Strike-.192f,Strike,t),width);
            }
            else if(group==25)
            {
                s=Vector3.one*(t<Strike || t>=ClearAt?Hidden:Mathf.Max(Hidden,2f*ImpactRadius(t)));
            }
            else if(group>=26 && group<=29)
            {
                // One restrained strike burst: two outward fronts and two descending rings.
                int ring=group-26;
                float age=t-Strike-(ring%2)*.45f;
                if(age<0f || age>=(ring<2?3.2f:1.8f))
                    return new Pose(Vector3.zero,Vector3.one*Hidden,Quaternion.identity);
                float open=Smoother(0f,.12f,age);
                float radius;
                if(ring<2)
                {
                    radius=Mathf.Lerp(3f,32f,Smoother(0f,3.2f,age));
                    p=new Vector3(0f,Sample(5,t).Position.y-1.5f-ring*.8f,0f);
                }
                else
                {
                    float travel=Mathf.Clamp01(age/1.8f);
                    radius=Mathf.Lerp(2.2f,3.2f,travel);
                    p=new Vector3(0f,Mathf.Lerp(Sample(5,t).Position.y,.15f,travel),0f);
                }
                s=Iris(Vector3.one,radius*open);
            }
            else if((group>=22 && group<=24) || group==30 || group==31)
            {
                // Position and scale stay neutral so traced endpoints remain on the crystals.
                s=Vector3.one*(t<Shimmer || t>=ClearAt?Hidden:1f);
            }
            else if(group>=32 && group<=36)
            {
                int i=group-32;
                float reveal=Smoother(RiserCues[i],Arrival,t);
                float live=CrystalBeamVisibility(t,true);
                return BeamPose(ReflectionPoint(5+i,t),StarConnection(i,t),reveal,
                    (1.1f+1.6f*ConvergenceBoost(t))*live);
            }
            else // Capacitor pylons: floating crystals that charge, discharge and ring the descending plate.
            {
                // On its anchor collar's bearing but OUTBOARD of it: r 21.5 puts the crystal 1.6 local
                // (3.8 m) clear of the disc's rim, in line with the collar and the mains conduit that
                // rises from the collar's centre - which is why it cannot stand centred on the disc.
                // It holds that radius for the whole show, so it never sweeps through the station, and
                // it FLOATS CrystalLift over the deck from the start: at deck height the crystal and its
                // collar stack up in parallax and read as one object with the crystal off centre.
                bool inner=group>=16 && group<=20;
                int i=inner?group-16:group-37;
                float bearing=i*72f+(inner?36f:0f), a=bearing*Mathf.Deg2Rad, cue=ThudCues[i]+.35f+(inner?.8f:0f);
                float g=CrystalReveal(t,inner?5+i:i);
                float rad=21.5f*(inner?110f/218f:1f);
                rad*=1f+.08f*EaseIn(ExitCues[3],ExitCues[3]+3f,t,2.2f);
                float y=DeckY+CrystalLift+2.8f*g+.12f*Smoother(Pressure,Cut,t); // heel pin
                // INVERSION lifts them to the middle tier and they ride journey 3 down with the plate:
                // tip 0.5 under its lowest layer, outside its rim, and heel still over the deck when
                // the plate seats at SealSeatY.
                y=Mathf.Lerp(y,stackY-3.6f,Smoother(Escalate2,100f,t));
                p=new Vector3(Mathf.Sin(a)*rad,y,Mathf.Cos(a)*rad);
                int slot=i*2+(inner?1:0);
                // Invert a quadratic activation curve so each successive crystal joins sooner.
                float start=18f+40f*Mathf.Sqrt(slot/9f);
                float end=start+6.6f;
                float settle=Smoother(start,end,t);
                p=Vector3.Lerp(ScatteredCrystal(inner?5+i:i),p,settle);
                float converge=Smoother(Escalate2,RecedeAt+.35f,t);
                p=Vector3.Lerp(p,Sample(5,t).Position,converge);
                float consumed=Smoother(.4f,1.6f,Vector3.Distance(p,Sample(5,t).Position));
                s=Grow(Vector3.one*(g*(inner?.62f:1f)),dissolve*consumed);
                r=Quaternion.Slerp(Quaternion.Euler(0f,(inner?5+i:i)*36f,0f),Quaternion.Euler(0f,bearing,0f),settle);
            }
            return new Pose(p,s,r);
        }

        public static float Whiteout(float t) => Smooth(WhiteoutAt,ClearAt,t);

        // Local radius; the final front expands monotonically toward the surrounding viewers.
        public static float ImpactRadius(float t)
        {
            float held=2.2f*Smoother(Strike,Strike+.22f,t);
            return Mathf.Lerp(held,250f,Smoother(RecedeAt,WhiteoutAt,t));
        }

        public static float ViewerWhiteout(float t,float cameraDistance)
            => t<RecedeAt?0f:Smooth(cameraDistance,cameraDistance+1f,ImpactRadius(t+.15f)*WorldScale);

        public const float SeedRise=2f, SeedOpen=12f, SeedClear=14.4f;
        // The expanding ring reaches the initial crystal circle (radius 19) at this time.
        public const float CrystalSeedContact=13.542022f;
        // Deterministic smooth noise gives irregular flicker without discontinuous scale jumps.
        public static float FlickerScale(int voice,float t)
        {
            float clock=t*(5.3f+voice*.137f)+voice*3.71f;
            float cell=Mathf.Floor(clock),phase=clock-cell;
            float a=Mathf.Sin(cell*12.9898f+voice*7.23f)*43758.5453f;
            float b=Mathf.Sin((cell+1f)*12.9898f+voice*7.23f)*43758.5453f;
            a-=Mathf.Floor(a);b-=Mathf.Floor(b);
            float noise=Mathf.Lerp(a,b,phase*phase*(3f-2f*phase));
            return .28f+.95f*noise*(.8f+.2f*Mathf.Sin(t*1.13f+voice));
        }
        public static Vector3 ScatteredCrystal(int index)
        {
            float angle=index*36f*Mathf.Deg2Rad;
            return new Vector3(Mathf.Sin(angle)*19f,DeckY+CrystalLift+2.8f,Mathf.Cos(angle)*19f);
        }
        public static float CrystalReveal(float t,int index)
        {
            float touch=Smoother(CrystalSeedContact-.1f,CrystalSeedContact+.06f,t);
            float grow=Smoother(CrystalSeedContact+.08f,CrystalSeedContact+1.05f,t);
            return touch*Mathf.Lerp(.12f,1f,grow);
        }
        public static Color FinalFlareColour(float t,float radiance)
        {
            float shift=Smoother(RecedeAt-.9f,RecedeAt+.3f,t)*(1f-Smoother(WhiteoutAt-.55f,WhiteoutAt,t));
            return Color.Lerp(new Color(radiance,radiance*1.25f,radiance*1.5f,1f),new Color(radiance*1.4f,radiance*.35f,radiance*1.8f,1f),shift);
        }
        public static bool TryReadAppearance(string name,out int kind,out int index)
        {
            kind=index=0;
            var p=name.Split('_');
            if(p.Length<3 || !int.TryParse(p[1],out index))return false;
            kind=p[0]=="pulse"?1:p[0]=="fade"?2:p[0]=="tint"?3:0;
            return kind!=0;
        }
        public static float AppearanceGain(int kind,int index,float t)
        {
            float gain=1f;
            if(t>=ClearAt)return 0f;
            if(kind==3)return Mathf.Round(Smoother(RecedeAt-1.8f,RecedeAt-.4f,t)*(1f-Smoother(WhiteoutAt-.55f,WhiteoutAt,t))*8f)/8f;
            if(kind==1)
            {
                if(index<10 && CrystalReveal(t,index)<=0f)return 0f;
                if(index==10 && (t<SeedRise || t>=SeedOpen+2f))return 0f;
                gain=1f;
            }
            else if(index==43)gain=1f-Smoother(SeedClear-.6f,SeedClear,t);
            else
            {
                float age=t-Strike-((index-26)%2)*.45f;
                float end=index<28?3.2f:1.8f;
                gain=1f-Smoother(end-.7f,end,age);
            }
            return Mathf.Round(gain*8f)/8f;
        }

        public static int CrystalGroup(int point) => point<5?37+point:16+point-5;
        public static Vector3 ReflectionPoint(int point,float t) => Sample(CrystalGroup(point),t).Position;
        public static Vector3 ConvergencePoint(float t) => new Vector3(0f,StackY(t)+4f*Gap(t),0f);
        public static Vector3 StarConnection(int ray,float t)
        {
            var star=Sample(5,t);
            float angle=ray*72f*Mathf.Deg2Rad;
            var tip=new Vector3(Mathf.Sin(angle)*6.3f,0f,Mathf.Cos(angle)*6.3f);
            return star.Position+star.Rotation*Vector3.Scale(tip,star.Scale);
        }

        // Trace metadata is carried in exported primitive names, shared by authoring and native playback.
        public static bool TryReadTrace(string name,out int track,out int index,out int count)
        {
            track=index=count=0;
            if(!name.StartsWith("trace_",System.StringComparison.Ordinal))return false;
            string[] parts=name.Split('_');
            return parts.Length>=4 && int.TryParse(parts[1],out track) && int.TryParse(parts[2],out index)
                && int.TryParse(parts[3],out count) && track>=0 && track<=9 && index>=0 && index<count;
        }

        public static float TraceReveal(int track,int index,int count,float t)
        {
            float start,end;
            if(track==7)
            {
                // The last coloured edge arrives exactly as its destination emits white.
                float depart=ReflectionDeparture(index);
                return Smooth(depart,depart+.8f,t);
            }
            else if(track==0){start=BreechLoad;end=38.677333f;}
            else if(track==1){start=Impact;end=FanOut[0]+FanDwell[0];}
            else {start=Shimmer+(track-2)*.4f+.7f;end=start+1.6f;}
            float step=(end-start)/count;
            return Smooth(start+index*step,start+(index+1)*step,t);
        }

        public static Pose SampleTrace(int track,int index,int count,Vector3 restPosition,
            Quaternion restRotation,Vector3 restScale,float t)
        {
            if(track==9)
            {
                float pulse=FlickerScale(index,t);
                if(t<CrystalSeedContact+1.15f)
                {
                    // Reuse each crystal core as the light deposited by the ring, then let
                    // its facets grow out around it. Compensate for the growing parent.
                    float touch=Smoother(CrystalSeedContact-.1f,CrystalSeedContact+.06f,t);
                    float resolve=Smoother(CrystalSeedContact+.18f,CrystalSeedContact+1.15f,t);
                    pulse=touch*Mathf.Lerp(2.4f,pulse,resolve)/Mathf.Max(.04f,CrystalReveal(t,index));
                }
                return new Pose(restPosition,Grow(restScale,pulse),restRotation);
            }
            if(track==8)
                return new Pose(restPosition,t<Strike?Vector3.one*Hidden:restScale,restRotation);
            float reveal=TraceReveal(track,index,count,t);
            Vector3 from,to;float width=restScale.x,depth=restScale.y;
            if(track==7)
            {
                int a=ReflectionVertex(index/ReflectionLegs,index%ReflectionLegs),b=ReflectionVertex(index/ReflectionLegs,index%ReflectionLegs+1);
                from=ReflectionPoint(a,t);to=ReflectionPoint(b,t);
                // Retire the previous leg as its successor arrives; keep the last leg through deployment.
                float live=CrystalBeamVisibility(t);
                if(index%ReflectionLegs<ReflectionLegs-1)
                {
                    float fade=ReflectionDeparture(index+1)+.8f;
                    live*=1f-Smoother(fade,fade+.6f,t);
                }
                width=depth=(a<5 && b<5?.10f:.14f)
                    *(1f+.25f*ConvergenceBoost(t))*live;
            }
            else
            {
                Vector3 half=restRotation*new Vector3(0f,0f,restScale.z*.5f);
                from=restPosition-half;to=restPosition+half;
            }
            if(reveal<=0f || width<=0f)return new Pose(from,Vector3.one*Hidden,restRotation);
            Vector3 delta=to-from;
            float length=Mathf.Sqrt(delta.x*delta.x+delta.y*delta.y+delta.z*delta.z);
            return new Pose(from+delta*(reveal*.5f),new Vector3(width,depth,Mathf.Max(Hidden,length*reveal)),
                track==7?Quaternion.LookRotation(delta):restRotation);
        }

        // After deployment, the coloured paths shrink first; convergence follows afterward.
        static float CrystalBeamVisibility(float t,bool convergence=false)
        {
            const float duration=.8f;
            float start=FanOut[0]+FanDwell[0]+(convergence?duration:0f);
            return 1f-Smoother(start,start+duration,t);
        }

        // Unit-Z geometry grows from its source; both ends follow the same moving anchors.
        static Pose BeamPose(Vector3 from,Vector3 to,float reveal,float width)
        {
            if(reveal<=0f || width<=.0001f)return new Pose(from,Vector3.one*Hidden,Quaternion.identity);
            Vector3 delta=to-from;
            float length=Mathf.Sqrt(delta.x*delta.x+delta.y*delta.y+delta.z*delta.z);
            if(length<=.0001f)return new Pose(from,Vector3.one*Hidden,Quaternion.identity);
            return new Pose(from,new Vector3(width,width,length*reveal),Quaternion.LookRotation(delta));
        }

        public static float ConvergenceBoost(float t)
        {
            if(t<Arrival || t>=ClearAt)return 0f;
            float energy=.20f*Smoother(Arrival,Impact,t);
            energy+=1.4f*Impulse(Impact-.10f,t,.10f,1.8f);
            energy+=1.4f*Impulse(Strike-.10f,t,.10f,1.8f);
            energy+=1.8f*Smoother(RecedeAt,WhiteoutAt,t);
            return energy*(1f-Smoother(WhiteoutAt,ClearAt,t));
        }

        // Nine shared HDR levels bound the native per-colour material cache and network updates.
        public static float ConvergenceRadiance(float t)
            => 8f+248f*Mathf.Round(Mathf.Clamp01((ConvergenceBoost(t)-.2f)/1.4f)*8f)/8f;

        public static float StrikeLightIntensity(float t) => 24000f*ConvergenceBoost(t);
        public static float StrikeLightRange(float t) => 150f+250f*Smoother(RecedeAt,WhiteoutAt,t);

        // Shared light gain; broad build ramps and the two principal impact accents.
        public const float LightGain = 2f;

        public static float LightIntensity(int light, float t)      // light 0-4 = stations, 5 = axis
        {
            if(t>=ClearAt)return 0f;
            float axis=light==5?1f:0f;
            float lead=2.8f*Smoother(0f,BarPhase,t);
            float mains=37f*Smoother(ThudCues[0],Shimmer,t);
            float charge=50f*Smoother(Charge,Pressure,t);
            float still=axis*310f*Smoother(Cut,Launch,t);            // the axis light alone, in the silence
            float riser=axis*90f*Smoother(Launch,Arrival,t);
            float hit=t>=FlashAt?900f*Mathf.Exp(-(t-FlashAt)*1.7f):0f;
            float waves=380f*Impulse(Strike,t,.08f,1.1f);
            return LightGain*(lead+mains+charge+still+riser+hit+waves)*(1f-EaseIn(RecedeAt,WhiteoutAt,t,2f));
        }
        static float Quantize(float v, int steps) => Mathf.Round(v*steps)/steps;
    }
}
