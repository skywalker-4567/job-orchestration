import { useEffect, useState } from 'react';
import { getAllJobs, getExecutions } from '../services/api';

const STATUS_COLORS = {
  COMPLETED:   { color: '#a6e3a1', background: '#1e3a2e' },
  FAILED:      { color: '#f38ba8', background: '#3a1e2e' },
  IN_PROGRESS: { color: '#f9e2af', background: '#3a341e' },
  SUBMITTED:   { color: '#89b4fa', background: '#1e2a3a' },
  RUNNING:     { color: '#f9e2af', background: '#3a341e' },
  ASSIGNED:    { color: '#89dceb', background: '#1e303a' },
  TIMEOUT:     { color: '#fab387', background: '#3a2a1e' },
};

function StatusBadge({ status }) {
  const style = STATUS_COLORS[status] || { color: '#cdd6f4', background: '#313244' };
  return (
    <span style={{
      ...style,
      padding: '3px 10px',
      borderRadius: '12px',
      fontSize: '12px',
      fontWeight: '600',
    }}>
      {status}
    </span>
  );
}

function ExecutionRow({ exec }) {
  const duration = exec.startedAt && exec.completedAt
    ? `${Math.round((new Date(exec.completedAt) - new Date(exec.startedAt)))}ms`
    : '—';

  return (
    <tr style={{ borderTop: '1px solid #181825' }}>
      <td style={tdStyle}>{exec.attemptNumber}</td>
      <td style={tdStyle}><StatusBadge status={exec.status} /></td>
      <td style={tdStyle}>{exec.region}</td>
      <td style={{ ...tdStyle, fontFamily: 'monospace', fontSize: '11px', color: '#a6adc8' }}>
        {exec.workerId ?? '—'}
      </td>
      <td style={{ ...tdStyle, color: '#f38ba8', fontSize: '12px' }}>
        {exec.errorMessage ?? '—'}
      </td>
      <td style={tdStyle}>{duration}</td>
    </tr>
  );
}

function ExecutionTable({ jobId }) {
  const [executions, setExecutions] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getExecutions(jobId)
      .then(setExecutions)
      .finally(() => setLoading(false));
  }, [jobId]);

  if (loading) return (
    <div style={{ padding: '16px', color: '#a6adc8', fontSize: '13px' }}>Loading executions...</div>
  );

  if (!executions || executions.length === 0) return (
    <div style={{ padding: '16px', color: '#a6adc8', fontSize: '13px' }}>No executions found.</div>
  );

  return (
    <div style={{ padding: '12px 24px 20px', background: '#11111b' }}>
      <div style={{ color: '#89b4fa', fontSize: '12px', marginBottom: '10px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
        Executions
      </div>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
        <thead>
          <tr>
            {['Attempt', 'Status', 'Region', 'Worker ID', 'Error', 'Duration'].map(h => (
              <th key={h} style={{ ...thStyle }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {executions.map(e => <ExecutionRow key={e.executionId} exec={e} />)}
        </tbody>
      </table>
    </div>
  );
}

function JobRow({ job, expanded, onToggle }) {
  return (
    <>
      <tr
        onClick={onToggle}
        style={{ borderTop: '1px solid #313244', cursor: 'pointer', transition: 'background 0.15s' }}
        onMouseEnter={e => e.currentTarget.style.background = '#1e1e2e'}
        onMouseLeave={e => e.currentTarget.style.background = 'transparent'}
      >
        <td style={{ ...tdStyle, fontFamily: 'monospace', fontSize: '11px', color: '#a6adc8' }}>
          {job.id}
        </td>
        <td style={tdStyle}><StatusBadge status={job.status} /></td>
        <td style={tdStyle}>{job.taskType}</td>
        <td style={tdStyle}>{job.priority}</td>
        <td style={{ ...tdStyle, color: '#a6adc8', fontSize: '12px' }}>
          {new Date(job.createdAt).toLocaleString()}
        </td>
        <td style={{ ...tdStyle, color: '#89b4fa', fontSize: '18px', textAlign: 'center' }}>
          {expanded ? '▾' : '▸'}
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={6} style={{ padding: 0 }}>
            <ExecutionTable jobId={job.id} />
          </td>
        </tr>
      )}
    </>
  );
}

export default function JobList() {
  const [jobs, setJobs] = useState([]);
  const [expandedId, setExpandedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetch = () => {
      getAllJobs()
        .then(data => {
          const sorted = [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
          setJobs(sorted);
          setError(null);
        })
        .catch(() => setError('Failed to load jobs.'))
        .finally(() => setLoading(false));
    };

    fetch();
    const interval = setInterval(fetch, 5000);
    return () => clearInterval(interval);
  }, []);

  if (loading) return (
    <div style={{ color: '#a6adc8', padding: '20px' }}>Loading jobs...</div>
  );

  if (error) return (
    <div style={{ color: '#f38ba8', padding: '20px' }}>{error}</div>
  );

  if (jobs.length === 0) return (
    <div style={{ color: '#a6adc8', padding: '20px', fontStyle: 'italic' }}>
      No jobs yet — submit a job to see activity.
    </div>
  );

  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
        <thead>
          <tr style={{ background: '#11111b' }}>
            {['Job ID', 'Status', 'Task Type', 'Priority', 'Created At', ''].map(h => (
              <th key={h} style={thStyle}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {jobs.map(job => (
            <JobRow
              key={job.id}
              job={job}
              expanded={expandedId === job.id}
              onToggle={() => setExpandedId(expandedId === job.id ? null : job.id)}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}

const thStyle = {
  padding: '10px 14px',
  textAlign: 'left',
  color: '#a6adc8',
  fontSize: '12px',
  fontWeight: '600',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  whiteSpace: 'nowrap',
};

const tdStyle = {
  padding: '12px 14px',
  color: '#cdd6f4',
  verticalAlign: 'middle',
  whiteSpace: 'nowrap',
};