import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import sys

dfs = []

acceptance_rate_label = 'acceptance_rate'
is_emerged_label = 'is_emerged'
is_subsumed_label = 'is_subsumed_by_emerged'
results_path = 'results/'

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sims = sim_string.split(',')

trial = sys.argv[4]
no_of_trials = 1
#trials_string = sys.argv[4]
#trials = trials_string.split(',')
#no_of_trials = len(trials)

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

for sim in sims:
    file_name = results_path + 'Results_Sim' + sim + get_ratio_suffix() + get_payscheme_suffix() \
            + get_trial_suffix(trial) + '_norms_data.csv'
    print(f'Reading file = %s' % file_name)
    try:
        dfs.append(pd.read_csv(file_name))
    except:
        dfs.append(None)

#for trial in trials:
#    file_name = results_path + 'Results_Sim' + agent_string + get_ratio_suffix() + get_payscheme_suffix() \
#            + get_trial_suffix(trial) + '_norms_data.csv'
#    print(f'Reading file = %s' % file_name)
#    try:
#        dfs.append(pd.read_csv(file_name))
#    except:
#        dfs.append(None)

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

label_map = {'15' : 'SIGA without explanation', '17': 'SIGA with explanation', '1': 'Fixed'}

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


for df in dfs:
    df[acceptance_rate_label] = df[acceptance_rate_label] * 100

fig, ax = plt.subplots()
ax.set(ylabel = 'Adoption (%)')

#x = [label_map[sim] for sim in sims]

data_map = dict()
for i in range(len(sims)):
    sim = sims[i]
    data_map[label_map[sim]] = dfs[i][acceptance_rate_label]

res_df = pd.DataFrame(data_map)

#ax.violinplot(dataset = [dfs[0][acceptance_rate_label], dfs[1][acceptance_rate_label]], positions = [1,2])
ax.violinplot(dataset = res_df, quantiles = [[0.0, 0.25, 0.5, 0.75, 1], [0.0, 0.25, 0.5, 0.75, 1]])
#ax.violinplot(dataset = res_df)

#fig.legend(loc = 'upper right')
ax.set_xticks([1, 2])
ax.set_xticklabels(res_df.columns)
plt.show()
