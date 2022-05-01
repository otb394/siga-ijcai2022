import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import sys

dfs = []

#step_label = 'Step' 
#avg_payoff = 'Avg Payoff in Window'
acceptance_rate_label = 'acceptance_rate'
is_emerged_label = 'is_emerged'
is_subsumed_label = 'is_subsumed_by_emerged'
results_path = 'results/'

#no_of_simulations = 6
#simulations = []

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

agent_string = sys.argv[3]

trials_string = sys.argv[4]
trials = trials_string.split(',')
no_of_trials = len(trials)

#sim_string = sys.argv[3]
#sim_tokens = sim_string.split(',')

#if len(sim_tokens) == 1:
#    simulations = range(1, int(sim_tokens[0]) + 1)
#else:
#    simulations = [int(x) for x in sim_tokens]
#no_of_simulations = len(simulations)

def get_ratio_suffix():
    return '_' + '_'.join(ratio)

def get_payscheme_suffix():
    if len(sys.argv) > 2 and sys.argv[2] == '2':
        return '_updated_base'
    elif len(sys.argv) > 2 and sys.argv[2] == '3':
        return '_multi_updated_base'
    else:
        return ''

def get_trial_suffix(trial_id):
    if (trial_id == '0'):
        return ''
    else:
        return '_trial' + trial_id

for trial in trials:
    file_name = results_path + 'Results_Sim' + agent_string + get_ratio_suffix() + get_payscheme_suffix() \
            + get_trial_suffix(trial) + '_norms_data.csv'
    print(f'Reading file = %s' % file_name)
    try:
        dfs.append(pd.read_csv(file_name))
    except:
        dfs.append(None)

#if len(sys.argv) > 2 and sys.argv[2] == '2':
#    suffix = '_' + '_'.join(ratio) + '_updated_base' + '.csv'
#elif len(sys.argv) > 2 and sys.argv[2] == '3':
#    suffix = '_' + '_'.join(ratio) + '_multi_updated_base' + '.csv'
#else:
#    suffix = '_' + '_'.join(ratio) + '.csv'

#for i in range(no_of_simulations):
#    #file_name = 'Results_Sim' + str(i+1) + suffix
#    sim_id = simulations[i]
#    file_name = results_path + 'Results_Sim' + str(sim_id) + suffix
#    #file_name = 'Results_Sim' + str(i+1) + '.csv'
#    print(f'Reading file = %s' % file_name)
#    try:
#        dfs.append(pd.read_csv(file_name))
#    except:
#        dfs.append(None)

#dfs = [x[[step_label, avg_payoff]] if x is not None else None for x in dfs]

labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
        'LCS', 'LCS Butz', 'LCS without explanation', 'LCS Butz with new explanation', 'LCS with explanation', \
        'LCS Butz without explanation + own norms']
colors = ['green', 'black', 'tab:blue', 'olive', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'red', 'coral', 'blue', 'tab:red']

#title = 'Average Payoffs for ratio ' + ':'.join(ratio) +  ' (Perfect:Selfish:Generous)'

#SMALL_SIZE = 14
#MEDIUM_SIZE = 14
#BIG_SIZE = 15
#BIGGER_SIZE = 16
#
#plt.rc('font', size=SMALL_SIZE)          # controls default text sizes
#plt.rc('axes', titlesize=BIGGER_SIZE)     # fontsize of the axes title
#plt.rc('axes', labelsize=BIGGER_SIZE)    # fontsize of the x and y labels
#plt.rc('xtick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
#plt.rc('ytick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
#plt.rc('legend', fontsize=BIG_SIZE)    # legend fontsize
#plt.rc('figure', titlesize=BIGGER_SIZE)  # fontsize of the figure title


fig, ax = plt.subplots()
ax.set(xlabel = 'Simulations', ylabel = 'Acceptance Rate (%)')

x = []
y = []
c = []

#subsumed_emerged_norm_color = 'tab:red'
subsumed_emerged_norm_color = 'tab:green'
#non_subsumed_emerged_norm_color = 'tab:green'
non_subsumed_emerged_norm_color = 'tab:red'
non_emerged_norm_color = 'tab:blue'

#legend_labels = ['Emergent Norm (dominant)', 'Emergent Norm (subsumed)', 'Non Emergent Norm']
legend_labels = ['Non Emergent Norm', 'Emergent Norm (subsumed)', 'Emergent Norm']

legend_colors = [non_emerged_norm_color, subsumed_emerged_norm_color,non_subsumed_emerged_norm_color]
legend_patches = []

#for i in range(len(legend_labels)):
#    legend_patches.append(mpatches.Patch(color = legend_colors[i], label = legend_labels[i]))

#for i in range(no_of_trials):
#    df = dfs[i]
#    #trial_id = int(trials[i])
#    if (df is not None):
#        for index, row in df.iterrows():
#            #x.append(trial_id)
#            acceptance_rate = row[acceptance_rate_label]
#            x.append(i + 1)
#            y.append(acceptance_rate * 100.0)
#            if (row[is_emerged_label] == True):
#                if (row[is_subsumed_label] == True):
#                    c.append(subsumed_emerged_norm_color)
#                else:
#                    c.append(non_subsumed_emerged_norm_color)
#            else:
#                c.append(non_emerged_norm_color)

for i in range(no_of_trials):
    df = dfs[i]
    #trial_id = int(trials[i])
    if (df is not None):
        for index, row in df.iterrows():
            #x.append(trial_id)
            acceptance_rate = row[acceptance_rate_label]
            if (row[is_emerged_label] == False or row[is_subsumed_label] == False):
                x.append(i + 1)
                y.append(acceptance_rate * 100.0)
                if (row[is_emerged_label] == True):
                    if (row[is_subsumed_label] == True):
                        c.append(subsumed_emerged_norm_color)
                    else:
                        c.append(non_subsumed_emerged_norm_color)
                else:
                    c.append(non_emerged_norm_color)

    #sim_id = simulations[i]
#    if df is not None:
#        ax.plot(df[step_label], df[avg_payoff], color = colors[sim_id - 1], label = labels[sim_id - 1])

def get_scatter_input(color):
    index = [i for i in range(len(x)) if (c[i] == color)]
    return [x[i] for i in index], [y[i] for i in index], [c[i] for i in index]

for i in range(len(legend_colors)):
    col = legend_colors[i]
    xx, yy, cc = get_scatter_input(col)
    if (len(xx) > 0):
        ax.scatter(xx, yy, c = cc, label = legend_labels[i])

#scatter = ax.scatter(x, y, c=c)
plt.xticks(range(1, no_of_trials + 1), range(1, no_of_trials + 1))


#ax.legend(handles = legend_patches, loc = 'upper right')
#fig.legend(handles = legend_patches, loc = 'upper right')
#plt.legend(handles = legend_patches, loc = 'upper right')
#plt.legend(handles = scatter.legend_elements()[0], labels = legend_labels)
#fig.legend(legend_colors, legend_labels)
#plt.legend(loc = 'lower right')
fig.legend(loc = 'upper right')
plt.show()
