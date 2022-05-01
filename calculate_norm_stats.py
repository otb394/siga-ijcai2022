import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import sys
from scipy.stats import ttest_rel
import pingouin as pt

acceptance_rate_label = 'acceptance_rate'
is_emerged_label = 'is_emerged'
is_subsumed_label = 'is_subsumed_by_emerged'
results_path = 'results/'

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sims = sim_string.split(',')

#trial = sys.argv[4]
#no_of_trials = 1
trials_string = sys.argv[4]
trials = trials_string.split(',')
no_of_trials = len(trials)

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

def get_stat_analysis_util(trial_sim_df):
    sims = trial_sim_df.columns
    baselines = sims[:-1]
    proposed = sims[-1]
    data = []
    for sim in baselines:
        row = []
        col_base = trial_sim_df[sim]
        col_prop = trial_sim_df[proposed]
        row.append(trial_sim_df[sim].min())
        row.append(trial_sim_df[sim].max())
        row.append(trial_sim_df[sim].mean())
        row.append(trial_sim_df[sim].std())
        stats_result = pt.ttest(col_base, col_prop, paired = True)
        row.append(stats_result['p-val']['T-test'])
        row.append(stats_result['cohen-d']['T-test'])
        data.append(row)
    data.append([trial_sim_df[proposed].min(), trial_sim_df[proposed].max(), trial_sim_df[proposed].mean(), trial_sim_df[proposed].std(), None, None])
    num_sims = [int(x) for x in sims]
    return pd.DataFrame(data, index = num_sims, columns = ['Min', 'Max', 'Mean', 'STD', 'p-value', 'cohen\'s d'])

trial_sim_data_all = []
trial_sim_data_emerged = []
for trial in trials:
    row_all = []
    row_emerged = []
    for sim in sims:
        file_name = results_path + 'Results_Sim' + sim + get_ratio_suffix() + get_payscheme_suffix() \
                + get_trial_suffix(trial) + '_norms_data.csv'
        print(f'Reading file = %s' % file_name)
        df = pd.read_csv(file_name)
        adoption_col = df[acceptance_rate_label]
        adoption_col = 100 * adoption_col
        row_all.append(adoption_col.mean())
        row_emerged.append(adoption_col[lambda x: x >= 90].mean())
    trial_sim_data_all.append(row_all)
    trial_sim_data_emerged.append(row_emerged)

trial_sim_df_all = pd.DataFrame(trial_sim_data_all, columns = sims)
trial_sim_df_emerged = pd.DataFrame(trial_sim_data_emerged, columns = sims)

#print(trial_sim_df_all)
print(trial_sim_df_emerged)

res_all = get_stat_analysis_util(trial_sim_df_all)
res_emerged = get_stat_analysis_util(trial_sim_df_emerged)

print("===============")
print("all")
print(res_all.round(6))

print("===============")
print("emerged")
print(res_emerged.round(6))

#labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
#        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
#        'LCS', 'LCS Butz', 'LCS without explanation', 'LCS Butz with new explanation', 'LCS with explanation', \
#        'LCS Butz without explanation + own norms']
#colors = ['green', 'black', 'tab:blue', 'olive', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'red', 'coral', 'blue', 'tab:red']
#
#label_map = {'15' : 'SIGA without explanation', '17': 'SIGA with explanation', '1': 'Fixed'}

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


#for df in dfs:
#    df[acceptance_rate_label] = df[acceptance_rate_label] * 100
#
#fig, ax = plt.subplots()
#ax.set(ylabel = 'Adoption (%)')
#
##x = [label_map[sim] for sim in sims]
#
#data_map = dict()
#for i in range(len(sims)):
#    sim = sims[i]
#    data_map[label_map[sim]] = dfs[i][acceptance_rate_label]
#
#res_df = pd.DataFrame(data_map)
#
##ax.violinplot(dataset = [dfs[0][acceptance_rate_label], dfs[1][acceptance_rate_label]], positions = [1,2])
#ax.violinplot(dataset = res_df, quantiles = [[0.0, 0.25, 0.5, 0.75, 1], [0.0, 0.25, 0.5, 0.75, 1]])
##ax.violinplot(dataset = res_df)
#
##fig.legend(loc = 'upper right')
#ax.set_xticks([1, 2])
#ax.set_xticklabels(res_df.columns)
#plt.show()
