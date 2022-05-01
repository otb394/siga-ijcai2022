package simulation;

import base.Action;
import base.Context;
import base.Relationship;
import util.Debugger;

import java.io.BufferedReader;
import java.io.FileReader;

public class UpdatedBasePayoffCalculator extends PayoffCalculator {
    //pay off table, if neighbor thinks callee should answer
    private double[] payoff_a;
    //pay off table, if neighbor thinks callee should ignore
    //during first and second simulations, neighbor thinks callee should ignore
    //during a meeting or in a library (hard-coded in this way).
    private double[] payoff_i;

    public UpdatedBasePayoffCalculator() {
        payoff_a = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,1,-1,0,0,1,-1,0,0};
        payoff_i = new double[]{1,0,2,-1,0,0.5,1,-0.5,1,-1,2,-2,0,0,-1,1,0,0,-1,1,0,0};
        try{
            BufferedReader reader = new BufferedReader(new FileReader("payoff_updated.txt"));
            String line;
            String[] items;
            int i = 0;
            while((line=reader.readLine())!=null){
                if (i>=payoff_a.length) break;
                line = line.trim();
                if (line.length()<=0) continue;
                if (line.startsWith("#")) continue;
                items = line.split("\\s+");
                payoff_a[i] = Double.parseDouble(items[0]);
                try{
                    payoff_i[i] = Double.parseDouble(items[1]);
                }catch(Exception ee){
                    payoff_i[i] = payoff_a[i];
                }
                i++;
            }
            reader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        Debugger.debug(payoff_a, "payoff_a");
        Debugger.debug(payoff_i, "payoff_i");
    }

    @Override
    public double calculateNeighborPayoff(Context context, Action calleeAction, boolean neighborAccept) {
        switch (context.calleeLocation) {
            case HOME:
                if (calleeAction == Action.RING) {
                    if (neighborAccept) {
                        return payoff_a[12];
                    } else {
                        return payoff_i[12];
                    }
                } else {
                    if (!neighborAccept) {
                        return payoff_a[13];
                    } else {
                        return payoff_i[13];
                    }
                }
            case MEETING:
                if (calleeAction == Action.RING) {
                    if (neighborAccept) {
                        return payoff_a[14];
                    } else {
                        return payoff_i[14];
                    }
                } else {
                    if (!neighborAccept) {
                        return payoff_a[15];
                    } else {
                        return payoff_i[15];
                    }
                }
            case PARTY:
                if (calleeAction == Action.RING) {
                    if (neighborAccept) {
                        return payoff_a[16];
                    } else {
                        return payoff_i[16];
                    }
                } else {
                    if (!neighborAccept) {
                        return payoff_a[17];
                    } else {
                        return payoff_i[17];
                    }
                }
            case LIBRARY:
                if (calleeAction == Action.RING) {
                    if (neighborAccept) {
                        return payoff_a[18];
                    } else {
                        return payoff_i[18];
                    }
                } else {
                    if (!neighborAccept) {
                        return payoff_a[19];
                    } else {
                        return payoff_i[19];
                    }
                }
            case ER:
                if (calleeAction == Action.RING) {
                    if (neighborAccept) {
                        return payoff_a[20];
                    } else {
                        return payoff_i[20];
                    }
                } else {
                    if (!neighborAccept) {
                        return payoff_a[21];
                    } else {
                        return payoff_i[21];
                    }
                }
            default: throw new RuntimeException("Unrecognized location");
        }
    }

    @Override
    public double calculateCalleePayoff(Context context, Action calleeAction) {
        if (context.callerRelationship != Relationship.STRANGER) {
            if (!context.callUrgency) {
                if (calleeAction == Action.RING) {
                    return payoff_a[0];
                } else {
                    return payoff_a[1];
                }
            } else {
                if (calleeAction == Action.RING) {
                    return payoff_a[2];
                } else {
                    return payoff_a[3];
                }
            }
        } else {
            if (!context.callUrgency) {
                if (calleeAction == Action.RING) {
                    return payoff_a[4];
                } else {
                    return payoff_a[5];
                }
            } else {
                if (calleeAction == Action.RING) {
                    return payoff_a[6];
                } else {
                    return payoff_a[7];
                }
            }
        }
    }

    @Override
    public double calculateCallerPayoff(Context context, Action calleeAction) {
        if (!context.callUrgency) {
            if (calleeAction == Action.RING) {
                return payoff_a[8];
            } else {
                return payoff_a[9];
            }
        } else {
            if (calleeAction == Action.RING) {
                return payoff_a[10];
            } else {
                return payoff_a[11];
            }
        }
    }
}
