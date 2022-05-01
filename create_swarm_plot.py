import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import sys
import seaborn as sns

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

#labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
#        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
#        'LCS', 'LCS Butz', 'LCS without explanation', 'LCS Butz with new explanation', 'LCS with explanation', \
#        'LCS Butz without explanation + own norms']
label_map = {'15' : 'NSIGA', '17': 'XSIGA', '1': 'Fixed', '3': 'Poros'}
#color_map = {'15' : 'tab:blue', '17': 'tab:orange', '1': 'tab:green', '3': 'tab:red'}
color_map = {'15' : 'red', '17': 'brown', '1': 'blue', '3': 'tab:red'}
#colors = ['green', 'black', 'tab:blue', 'olive', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'red', 'coral', 'blue', 'tab:red']
cat_label = 'category'

for df in dfs:
    df[acceptance_rate_label] = df[acceptance_rate_label] * 100


#plt.rcParams.update({'font.size': 12})
#sns.set(font_scale = 1.2)
plt.rcParams["font.family"] = "Times New Roman" # set font to Times New Roman
plt.rcParams["font.size"] = 11
fig, ax = plt.subplots()
#ax.set(xlabel = 'Type of Agent', ylabel = 'Adoption (%)')
#fig.set_size_inches(3.5, 1.77)
fig.set_size_inches(2.83, 1.77)
fig.set_dpi(256)
#ax.set(ylabel = 'Adoption (%)', fontsize = 8)
ax.set_ylabel('Adoption (%)', fontsize = 11)
plt.xticks(fontsize = 11)
plt.yticks(fontsize = 11)

data_map = dict()
for i in range(len(sims)):
    sim = sims[i]
    data_map[label_map[sim]] = dfs[i][acceptance_rate_label]

res_df = pd.DataFrame(data_map)

print(res_df)

#x = [labels[int(sim)] for sim in sims]
#sns.swarmplot(x=x, y = [dfs[0][acceptance_rate_label], dfs[1][acceptance_rate_label]])
paletteDict = dict()
paletteDict['NSIGA'] = 'tab:red'
paletteDict['XSIGA'] = 'tab:blue'
sns.swarmplot(x="", y="Adoption (%)", data = res_df.melt(var_name = "", value_name = "Adoption (%)"), hue = "", s = 2, palette=paletteDict)
#sns.swarmplot(data = res_df, s = 2)
plt.legend([],[], frameon=False)
plt.tight_layout()
plt.show()
