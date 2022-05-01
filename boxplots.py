import pandas as pd
import matplotlib.pyplot as plt
import sys

dfs = []

step_label = 'Step' 
avg_payoff = 'Avg Payoff in Window'

agent_types = ['perfect', 'selfish', 'generous']

#agent_payoff_labels = ["Avg Expected Payoff for {} agents in Window".format(x) for x in agent_types]
agent_payoff_labels = ["Expected Payoff for {} agents".format(x) for x in agent_types]

file_name = sys.argv[1]

df = pd.read_csv(file_name)

df = df[[step_label] + agent_payoff_labels]

def get_graph_name(file_name):
    sim = get_simulation_type(file_name[11])
    rem = file_name[13:-4]
    ratio = rem.split('_')
    print(f'ratio = %s' % ratio)
    return sim + ' ' + ':'.join(ratio) + ' (Perfect:Selfish:Generous)', ratio


def get_simulation_type(sim):
    if (sim == 1):
        return 'Fixed'
    if (sim == 2):
        return 'Sanctioning'
    return 'Poros'

#dfs = [x[[step_label, avg_payoff] + agent_payoff_labels] for x in dfs]

#labels = ['Fixed', 'Sanctioning', 'Poros']
colors = ['green', 'red', 'blue', 'yellow']

## Plot the population payoffs


title, ratio = get_graph_name(file_name)
plot_nums = len([x for x in range(3) if ratio[x] != '0'])
fig, ax = plt.subplots(1, plot_nums, constrained_layout = True)
bp = [None] * plot_nums
#bp = [None] * 3
#ax.set(xlabel = 'Steps', ylabel = avg_payoff, title = title)

cnt = 0

for agentId in range(len(agent_types)):
    if (ratio[agentId] == '0'):
        continue
    agent_payoff_label = agent_payoff_labels[agentId]
    #ax.plot(df[step_label], df[agent_payoff_label], color = colors[agentId], label = agent_types[agentId])
    #ax[agentId].set_ylim([-0.1, 0.3])
    ax[cnt].set_ylim([-0.1, 0.3])
    #bp[agentId] = df[agent_payoff_label].plot.box(showfliers = True, ax = ax[agentId], patch_artist = True, return_type = 'dict')
    bp[cnt] = df[agent_payoff_label].plot.box(showfliers = True, ax = ax[cnt], patch_artist = True, return_type = 'dict')
    #temp = bp[agentId]
    temp = bp[cnt]

    boxes = temp['boxes']
    boxes[0].set_facecolor(colors[agentId])
    medians = temp['medians']
    medians[0].set_color('black')
    cnt = cnt + 1

legend_colors = [bp[y]['boxes'][0] for y in range(plot_nums)]

fig.legend(legend_colors, agent_types)
#fig.legend((bp[0]['boxes'][0], bp[1]['boxes'][0], bp[2]['boxes'][0]), agent_types)
#ax.legend()
plt.show()
