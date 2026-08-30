import com.wecca.canoeanalysis.models.canoe.Hull;
import com.wecca.canoeanalysis.utils.GirRaftPreset;
import com.wecca.canoeanalysis.utils.RaftPunkPreset;

public final class PresetAudit {
    public static void main(String[] args) {
        print("RAFT", RaftPunkPreset.createHull());
        print("RAFT_CAL", RaftPunkPreset.createHullWithAsBuiltMass());
        print("GIR", GirRaftPreset.createHull());
    }

    private static void print(String name, Hull hull) {
        System.out.printf(
                "%s length=%.9f width=%.9f height=%.9f concreteVol=%.9f totalVol=%.9f density=%.9f mass=%.9f weight=%.9f thickness=%.9f%n",
                name,
                hull.getLength(),
                hull.getMaxWidth(),
                hull.getMaxHeight(),
                hull.getConcreteVolume(),
                hull.getTotalVolume(),
                hull.getConcreteDensity(),
                hull.getMass(),
                hull.getWeight(),
                hull.getMaxThickness());
    }
}
