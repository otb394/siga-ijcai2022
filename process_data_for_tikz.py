import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.lines import Line2D
import sys

dfs = []

step_label = 'Step' 
avg_payoff = 'Avg Payoff in Window'
results_path = 'results/'

#no_of_simulations = 6
simulations = []

ratio_string = sys.argv[1]
ratio = ratio_string.split(',')

sim_string = sys.argv[3]
sim_tokens = sim_string.split(',')
if len(sim_tokens) == 1:
    simulations = range(1, int(sim_tokens[0]) + 1)
else:
    simulations = [int(x) for x in sim_tokens]
no_of_simulations = len(simulations)

def get_ratio_part():
    return '_' + '_'.join(ratio)

if len(sys.argv) > 2 and sys.argv[2] == '2':
    suffix = get_ratio_part() + '_updated_base' + '.csv'
    scheme_suffix = '_sch2'
elif len(sys.argv) > 2 and sys.argv[2] == '3':
    suffix = get_ratio_part() + '_multi_updated_base' + '.csv'
    scheme_suffix = '_sch3'
else:
    suffix = get_ratio_part() + '.csv'
    scheme_suffix = '_sch1'

for i in range(no_of_simulations):
    #file_name = 'Results_Sim' + str(i+1) + suffix
    sim_id = simulations[i]
    file_name = results_path + 'Results_Sim' + str(sim_id) + suffix
    #file_name = 'Results_Sim' + str(i+1) + '.csv'
    print(f'Reading file = %s' % file_name)
    try:
        dfs.append(pd.read_csv(file_name))
    except:
        dfs.append(None)

dfs = [x[[step_label, avg_payoff]] if x is not None else None for x in dfs]
dfs = [x[x[step_label] % 500 == 0] for x in dfs]

alt_step_label = 'timestep'
label_map = {'15' : 'NSIGA', '17': 'XSIGA', '1': 'Fixed', '3': 'Poros'}

ln = len(dfs)
if ln > 0:
    combined_df = dfs[0]
    sim_id = simulations[0]
    label = label_map[str(sim_id)]
    combined_df = combined_df.rename(columns={avg_payoff: label})
    for i in range(1, ln):
        sim_id = simulations[i]
        label = label_map[str(sim_id)]
        combined_df = combined_df.merge(dfs[i], on=step_label)
        combined_df = combined_df.rename(columns={avg_payoff: label})

print(combined_df)
combined_df.to_csv("processed_data_for_tikz_" + suffix, index = False)


#labels = ['Fixed', 'Sanctioning', 'Poros', 'Rule-Based RL with default', 'Rule-Based RL without explanation', 'StateRL base greedy', 'Rule-Based RL', 'LCS OG', 'LCS Sanctioning', \
#        'LCS Epsilong exploration', 'LCS Initial Only Epsilon exploration', 'LCS Initial Only alternating exploration', \
#        'LCS', 'LCS Butz', 'LCS without explanation', 'LCS Butz with new explanation', 'LCS with explanation', \
#        'LCS Butz without explanation + own norms']
#colors = ['green', 'black', 'tab:red', 'red', 'yellow', 'purple', 'aqua', 'maroon', 'cyan', 'brown', 'pink', 'gray', 'orange', 'lightcoral', 'olive', 'coral', 'tab:blue', 'blue']
#
#
#
#color_map = {'15' : 'tab:blue', '17': 'tab:orange', '1': 'tab:green', '3': 'tab:red'}
##color_map = {'15' : 'black', '17': 'black', '1': 'black', '3': 'tab:red'}
#style_map = {'15' : 'dashed', '17': 'dotted', '1': 'solid'}
#combined_style_map = {'15' : 'k--', '17': 'k:', '1': 'k-'}
#dash_map = {'15' : [5,5], '17': [1,5], '1': []}
#marker_map = {'15' : '^', '17': [1,5], '1': []}
#
##title = 'Average Payoffs for ratio ' + ':'.join(ratio) +  ' (Perfect:Selfish:Generous)'
#
##SMALL_SIZE = 14
##MEDIUM_SIZE = 14
##BIG_SIZE = 15
##BIGGER_SIZE = 16
##
##plt.rc('font', size=SMALL_SIZE)          # controls default text sizes
##plt.rc('axes', titlesize=BIGGER_SIZE)     # fontsize of the axes title
##plt.rc('axes', labelsize=BIGGER_SIZE)    # fontsize of the x and y labels
##plt.rc('xtick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
##plt.rc('ytick', labelsize=SMALL_SIZE)    # fontsize of the tick labels
##plt.rc('legend', fontsize=BIG_SIZE)    # legend fontsize
##plt.rc('figure', titlesize=BIGGER_SIZE)  # fontsize of the figure title
#
#plt.rcParams["font.family"] = "Times New Roman" # set font to Times New Roman
#
#
#fig, ax = plt.subplots()
##ax.set(xlabel = 'Steps', ylabel = 'Social Experience')
##fig.set_size_inches(3.5, 1.77)
##fig.set_size_inches(2.83, 1.338)
#fig.set_size_inches(2.83, 1.77)
##fig.set_size_inches(2.65, 1.4)
#fig.set_dpi(256)
##ax.set_xlabel('Steps', fontsize = 8)
##ax.set_ylabel('Social Experience', fontsize = 8)
#ax.set_xlabel('Steps', fontsize = 11)
#ax.set_ylabel('Social Experience', fontsize = 11)
#ax.set_ylim(-0.05,2.05)
#
##plt.figure(num = fig, figsize=(3,3), dpi = 80)
#
#for i in range(no_of_simulations):
#    df = dfs[i]
#    sim_id = simulations[i]
#    if df is not None:
#        #ax.plot(df[step_label], df[avg_payoff], color = colors[sim_id - 1], label = labels[sim_id - 1])
#        #ax.plot(df[step_label], df[avg_payoff], color = colors[sim_id - 1], label = label_map[str(sim_id)])
#        #ax.plot(df[step_label], df[avg_payoff], color = color_map[str(sim_id)], label = label_map[str(sim_id)], linewidth = 1, linestyle = style_map[str(sim_id)])
#        #ax.plot(df[step_label], df[avg_payoff], combined_style_map[str(sim_id)], dashes=dash_map[str(sim_id)], label = label_map[str(sim_id)], linewidth = 1)
#        #ax.plot(df[step_label], df[avg_payoff], 'k', dashes=dash_map[str(sim_id)], label = label_map[str(sim_id)], linewidth = 1)
#        #ax.plot(df[step_label], df[avg_payoff], label = label_map[str(sim_id)], linewidth = 1, linestyle = style_map[str(sim_id)])
#        #ax.plot(df[step_label], df[avg_payoff], color = color_map[str(sim_id)], dashes=dash_map[str(sim_id)], label = label_map[str(sim_id)], linewidth = 1, marker = ',')
#        ax.plot(df[step_label], df[avg_payoff], color = color_map[str(sim_id)], label = label_map[str(sim_id)], linewidth = 1, marker = ',')
#
#params = {'legend.fontsize': 10,
#          'legend.handlelength': 0.8,
#          'legend.columnspacing': 0.8,
#          'legend.handletextpad': 0.3,
#          #'legend.markerscale': 0.0002,
#          #'legend.labelspacing': 0.01
#          'legend.numpoints' : 1,
#          #"font.family": "Times New Roman",
#          }
#
#plt.rcParams.update(params)
#plt.xticks(fontsize = 8)
#plt.yticks(fontsize = 8)
##plt.legend(loc = 'lower right', prop = {'size': 7})
#
#legend_labels = [label_map[str(sim_id)] for sim_id in simulations]
##print("Legend labels")
##print(legend_labels)
#legend_dash_map = {'15' : [2,0.5,2,1], '17': [1,1], '1': []}
#legend_lines = [Line2D([0,1],[0,1], linestyle=style_map[str(sim_id)], color = color_map[str(sim_id)], dashes = legend_dash_map[str(sim_id)]) for sim_id in simulations]
#plt.legend(legend_lines, legend_labels, loc = 'lower center', ncol=3)
##plt.legend(loc = 'best')
#str_sims = [str(x) for x in simulations]
#ext = '.pdf'
##fig.savefig('SocialExperience' + get_ratio_part() + '___' + '_'.join(str_sims) + scheme_suffix + ext, dpi = 100)
#plt.tight_layout()
#plt.show()
